/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.execution.workspace.impl;

import com.google.common.io.Closer;
import org.gradle.api.internal.cache.CacheConfigurationsInternal;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.cache.CacheBuilder;
import org.gradle.cache.CacheCleanupStrategy;
import org.gradle.cache.CleanupAction;
import org.gradle.cache.DefaultCacheCleanupStrategy;
import org.gradle.cache.FileLock;
import org.gradle.cache.FileLockManager;
import org.gradle.cache.PersistentCache;
import org.gradle.cache.internal.InMemoryCacheDecoratorFactory;
import org.gradle.cache.internal.LeastRecentlyUsedCacheCleanup;
import org.gradle.cache.internal.SingleDepthFilesFinder;
import org.gradle.internal.execution.history.ExecutionHistoryStore;
import org.gradle.internal.execution.history.impl.DefaultExecutionHistoryStore;
import org.gradle.internal.execution.workspace.WorkspaceProvider;
import org.gradle.internal.file.FileAccessTimeJournal;
import org.gradle.internal.file.impl.SingleDepthFileAccessTracker;
import org.gradle.internal.hash.ClassLoaderHierarchyHasher;
import org.gradle.util.internal.GFileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.util.function.Function;

import static org.gradle.cache.FileLockManager.LockMode.Exclusive;
import static org.gradle.cache.FileLockManager.LockMode.OnDemandExclusive;
import static org.gradle.cache.FileLockManager.LockMode.OnDemandShared;
import static org.gradle.cache.internal.filelock.LockOptionsBuilder.mode;

public class DefaultImmutableWorkspaceProvider implements WorkspaceProvider, Closeable {
    private static final int DEFAULT_FILE_TREE_DEPTH_TO_TRACK_AND_CLEANUP = 1;

    private final SingleDepthFileAccessTracker fileAccessTracker;
    private final File baseDirectory;
    private final ExecutionHistoryStore executionHistoryStore;
    private final PersistentCache cache;
    private final FileLockManager fileLockManager;
    private final Closeable onClose;

    public static DefaultImmutableWorkspaceProvider withBuiltInHistory(
        CacheBuilder cacheBuilder,
        CacheBuilder buildInHistoryCacheBuilder,
        FileAccessTimeJournal fileAccessTimeJournal,
        InMemoryCacheDecoratorFactory inMemoryCacheDecoratorFactory,
        StringInterner stringInterner,
        ClassLoaderHierarchyHasher classLoaderHasher,
        CacheConfigurationsInternal cacheConfigurations,
        FileLockManager fileLockManager
    ) {
        return withBuiltInHistory(
            cacheBuilder,
            buildInHistoryCacheBuilder,
            fileAccessTimeJournal,
            inMemoryCacheDecoratorFactory,
            stringInterner,
            classLoaderHasher,
            DEFAULT_FILE_TREE_DEPTH_TO_TRACK_AND_CLEANUP,
            cacheConfigurations,
            fileLockManager
        );
    }

    public static DefaultImmutableWorkspaceProvider withBuiltInHistory(
        CacheBuilder cacheBuilder,
        CacheBuilder buildInHistoryCacheBuilder,
        FileAccessTimeJournal fileAccessTimeJournal,
        InMemoryCacheDecoratorFactory inMemoryCacheDecoratorFactory,
        StringInterner stringInterner,
        ClassLoaderHierarchyHasher classLoaderHasher,
        int treeDepthToTrackAndCleanup,
        CacheConfigurationsInternal cacheConfigurations,
        FileLockManager fileLockManager
    ) {
        PersistentCache executionHistoryCache = buildInHistoryCacheBuilder
            .withCleanupStrategy(createCacheCleanupStrategy(fileAccessTimeJournal, treeDepthToTrackAndCleanup, cacheConfigurations))
            .withLockOptions(mode(OnDemandExclusive))
            .open();
        return new DefaultImmutableWorkspaceProvider(
            cacheBuilder,
            fileAccessTimeJournal,
            cache -> new DefaultExecutionHistoryStore(() -> executionHistoryCache, inMemoryCacheDecoratorFactory, stringInterner, classLoaderHasher),
            treeDepthToTrackAndCleanup,
            cacheConfigurations,
            fileLockManager,
            executionHistoryCache
        );
    }

    public static DefaultImmutableWorkspaceProvider withExternalHistory(
        CacheBuilder cacheBuilder,
        FileAccessTimeJournal fileAccessTimeJournal,
        ExecutionHistoryStore executionHistoryStore,
        CacheConfigurationsInternal cacheConfigurations,
        FileLockManager fileLockManager
    ) {
        return new DefaultImmutableWorkspaceProvider(
            cacheBuilder,
            fileAccessTimeJournal,
            __ -> executionHistoryStore,
            DEFAULT_FILE_TREE_DEPTH_TO_TRACK_AND_CLEANUP,
            cacheConfigurations,
            fileLockManager,
            () -> {}
        );
    }

    private DefaultImmutableWorkspaceProvider(
        CacheBuilder cacheBuilder,
        FileAccessTimeJournal fileAccessTimeJournal,
        Function<PersistentCache, ExecutionHistoryStore> historyFactory,
        int treeDepthToTrackAndCleanup,
        CacheConfigurationsInternal cacheConfigurations,
        FileLockManager fileLockManager,
        Closeable onClose
    ) {
        PersistentCache cache = cacheBuilder
            .withCleanupStrategy(createCacheCleanupStrategy(fileAccessTimeJournal, treeDepthToTrackAndCleanup, cacheConfigurations))
            .withLockOptions(mode(OnDemandShared))
            .open();
        this.fileLockManager = fileLockManager;
        this.cache = cache;
        this.baseDirectory = cache.getBaseDir();
        this.fileAccessTracker = new SingleDepthFileAccessTracker(fileAccessTimeJournal, baseDirectory, treeDepthToTrackAndCleanup);
        this.executionHistoryStore = historyFactory.apply(cache);
        this.onClose = onClose;
    }

    private static CacheCleanupStrategy createCacheCleanupStrategy(FileAccessTimeJournal fileAccessTimeJournal, int treeDepthToTrackAndCleanup, CacheConfigurationsInternal cacheConfigurations) {
        return DefaultCacheCleanupStrategy.from(
            createCleanupAction(fileAccessTimeJournal, treeDepthToTrackAndCleanup, cacheConfigurations),
            cacheConfigurations.getCleanupFrequency()::get
        );
    }

    private static CleanupAction createCleanupAction(FileAccessTimeJournal fileAccessTimeJournal, int treeDepthToTrackAndCleanup, CacheConfigurationsInternal cacheConfigurations) {
        return new LeastRecentlyUsedCacheCleanup(
            new SingleDepthFilesFinder(treeDepthToTrackAndCleanup),
            fileAccessTimeJournal,
            cacheConfigurations.getCreatedResources().getRemoveUnusedEntriesOlderThanAsSupplier()
        );
    }

    @Override
    public <T> T withWorkspace(String path, WorkspaceAction<T> action) {
        System.out.println("Locking workspace " + baseDirectory.getName() + " with " + "DefaultImmutableWorkspaceProvider::" + System.identityHashCode(this) + "::" + ManagementFactory.getRuntimeMXBean().getName());
        return cache.withFileLock(() -> {
            System.out.println("Locking workspace: " + path);
            File workspace = new File(baseDirectory, path);
            GFileUtils.mkdirs(workspace);
            FileLock innerLock = fileLockManager.lock(workspace, mode(Exclusive), "Immutable workspace: " + workspace.getParentFile().getName() + "/" + workspace.getName());
            try {
                fileAccessTracker.markAccessed(workspace);
                return action.executeInWorkspace(workspace, executionHistoryStore);
            } finally {
                innerLock.close();
            }
        });
    }

    @Override
    public void close() {
        try (Closer closer = Closer.create()) {
            closer.register(onClose);
            closer.register(cache);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
