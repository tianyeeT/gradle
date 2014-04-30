/*
 * Copyright 2013 the original author or authors.
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
package org.gradle.nativebinaries.toolchain.internal.msvcpp;

import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Transformer;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.FileUtils;
import org.gradle.internal.hash.HashUtil;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.nativebinaries.language.rc.internal.WindowsResourceCompileSpec;
import org.gradle.nativebinaries.toolchain.internal.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WindowsResourceCompiler implements Compiler<WindowsResourceCompileSpec> {

    private final CommandLineTool commandLineTool;
    private final Transformer<WindowsResourceCompileSpec, WindowsResourceCompileSpec> specTransformer;
    private final CommandLineToolInvocation baseInvocation;

    WindowsResourceCompiler(CommandLineTool commandLineTool, CommandLineToolInvocation invocation, Transformer<WindowsResourceCompileSpec, WindowsResourceCompileSpec> specTransformer) {
        this.commandLineTool = commandLineTool;
        this.specTransformer = specTransformer;
        this.baseInvocation = invocation;
    }

    public WorkResult execute(WindowsResourceCompileSpec spec) {
        boolean didWork = false;
        boolean windowsPathLimitation = OperatingSystem.current().isWindows();
        MutableCommandLineToolInvocation invocation = baseInvocation.copy();
        spec = specTransformer.transform(spec);
        for (File sourceFile : spec.getSourceFiles()) {
            RcCompilerArgsTransformer argsTransformer = new RcCompilerArgsTransformer(sourceFile, windowsPathLimitation);
            invocation.setArgs(argsTransformer.transform(spec));
            invocation.setWorkDirectory(spec.getObjectFileDir());
            WorkResult result = commandLineTool.execute(invocation);
            didWork |= result.getDidWork();
        }
        return new SimpleWorkResult(didWork);
    }

    private static class RcCompilerArgsTransformer implements ArgsTransformer<WindowsResourceCompileSpec> {
        private final File inputFile;
        private boolean windowsPathLengthLimitation;

        public RcCompilerArgsTransformer(File inputFile, boolean windowsPathLengthLimitation) {
            this.inputFile = inputFile;
            this.windowsPathLengthLimitation = windowsPathLengthLimitation;
        }

        public List<String> transform(WindowsResourceCompileSpec spec) {
            List<String> args = new ArrayList<String>();
            args.add("/nologo");
            args.add("/fo");
            args.add(getOutputFile(spec).getAbsolutePath());
            for (String macroArg : new MacroArgsConverter().transform(spec.getMacros())) {
                args.add("/D" + macroArg);
            }
            args.addAll(spec.getAllArgs());
            for (File file : spec.getIncludeRoots()) {
                args.add("/I" + file.getAbsolutePath());
            }
            args.add(inputFile.getAbsolutePath());

            return args;
        }

        private File getOutputFile(WindowsResourceCompileSpec spec) {
            String outputFileName = FilenameUtils.getBaseName(inputFile.getName()) + ".res";
            String compactMD5 = HashUtil.createCompactMD5(inputFile.getAbsolutePath());
            File outputDirectory = new File(spec.getObjectFileDir(), compactMD5);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }
            File outputFile = new File(outputDirectory, outputFileName);
            return windowsPathLengthLimitation ? FileUtils.assertInWindowsPathLengthLimitation(outputFile) : outputFile;
        }
    }
}
