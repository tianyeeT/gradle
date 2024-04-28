plugins {
    id("gradlebuild.distribution.api-java")
    id("gradlebuild.publish-public-libraries")
}

description = """Persistent caches on disk and cross process locking.
    | Mostly for persisting Maps to the disk.
    | Also contains implementations for in-memory caches in front of the disk cache.
""".trimMargin()

errorprone {
    disabledChecks.addAll(
        "EmptyBlockTag", // 2 occurrences
        "LockNotBeforeTry", // 3 occurrences
        "NonAtomicVolatileUpdate", // 1 occurrences
        "StringCaseLocaleUsage", // 1 occurrences
        "ThreadLocalUsage", // 1 occurrences
        "UnusedMethod", // 2 occurrences
        "WaitNotInLoop", // 1 occurrences
    )
}

dependencies {
    api(projects.concurrent)
    api(projects.javaLanguageExtensions)
    api(projects.serialization)
    api(project(":build-operations"))
    api(project(":files"))

    api(libs.jsr305)

    implementation(projects.io)
    implementation(projects.time)

    implementation(libs.guava)
    implementation(libs.slf4jApi)
    implementation(libs.commonsIo)
    implementation(libs.commonsLang)

    testImplementation(projects.messaging)
    testImplementation(project(":core-api"))
    testImplementation(project(":functional"))
    testImplementation(testFixtures(project(":core")))

    testRuntimeOnly(project(":distributions-core")) {
        because("DefaultPersistentDirectoryCacheTest instantiates DefaultClassLoaderRegistry which requires a 'gradle-plugins.properties' through DefaultPluginModuleRegistry")
    }

    integTestImplementation(projects.messaging)

    integTestDistributionRuntimeOnly(project(":distributions-core"))
}
