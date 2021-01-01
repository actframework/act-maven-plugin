/**
 * Copyright (C) 2012-2015 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.actframework.maven;

/*-
 * #%L
 * act-maven-plugin
 * %%
 * Copyright (C) 2017 - 2018 ActFramework
 * %%
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
 * #L%
 */

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This is a refactored version of
 * RunClassInSeparateJvmMachine.java from the Ninja Web Framework
 *
 * Original source code can to.be found here:
 * https://github.com/ninjaframework/ninja/blob/develop/ninja-maven-plugin/src/main/java/ninja/build/DelayedRestartTrigger.java
 *
 * @author svenkubiak
 */
public class Runner {
    private static final Logger LOG = LogManager.get(Runner.class);
    private OutputStream outputStream;
    private StartedProcess startedProcess;
    private final String mainClass;
    private final String classpath;
    private final File mavenBaseDir;
    private final int jpdaPort;
    private final String jvmArgs;
    private final boolean test;
    private String lombok;



    public Runner(String mainClass, String classpath, File mavenBaseDir, int jpdaPort, String jvmArgs, boolean test, String lombok) {
        this.outputStream = System.out; //NOSONAR
        this.mainClass = mainClass;
        this.classpath = classpath;
        this.mavenBaseDir = mavenBaseDir;
        this.jpdaPort = test ? 0 : jpdaPort;
        this.jvmArgs = jvmArgs;
        this.test = test;
        this.lombok = lombok;
    }

    public OutputStream getOutput() {
        return outputStream;
    }

    public void setOutput(OutputStream output) {
        this.outputStream = output;
    }

    public StartedProcess getActiveProcess() {
        synchronized (this) {
            return this.startedProcess;
        }
    }

    public void setActiveProcess(StartedProcess activeProcess) {
        synchronized (this) {
            this.startedProcess = activeProcess;
        }
    }

    public void start() throws MojoExecutionException {
        synchronized (this) {
            try {
                if (this.startedProcess != null) {
                    this.startedProcess.getProcess().destroy();
                    this.startedProcess.getFuture().get();
                }
                this.startedProcess = startProcess();
                int exitCode = this.startedProcess.getProcess().waitFor();
                System.out.println("sub process returned with exit code: " + exitCode);
                if (exitCode != 0 && test) {
                    throw new MojoExecutionException("End to end test failed");
                }
            } catch (ExecutionException | InterruptedException | IOException e) {
                LOG.error("Something fishy happenend. Unable to cleanly restart!", e);
                LOG.error("You'll probably need to restart maven?");
            }
        }
    }

    private StartedProcess startProcess() throws IOException {
        ProcessExecutor processExecutor = buildProcessExecutor();
        return processExecutor.start();
    }

    @SuppressWarnings("all")
    private ProcessExecutor buildProcessExecutor() {
        List<String> commandLine = new ArrayList<>();
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

        commandLine.add(javaBin);

        if (lombok != null) {
            commandLine.add("-javaagent:" + lombok + "=ECJ");
        }

        if (jpdaPort > 0) {
            LOG.info("Listening for jpda connection at " + jpdaPort);
            commandLine.add("-Xdebug");
            String s = System.getProperty("suspend");
            if ($.bool(s)) {
                commandLine.add(String.format("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s -Djava.awt.headless=true", jpdaPort));
            } else {
                commandLine.add(String.format("-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=%s -Djava.awt.headless=true", jpdaPort));
            }
        }

        if (StringUtils.isNotBlank(jvmArgs)) {
            String _jvmArgs = jvmArgs;
            if (test && !_jvmArgs.contains("-Dprofile=")) {
                _jvmArgs += " -Dprofile=test";
            }
            List<String> args = S.fastSplit(_jvmArgs, " ");
            for (String arg : args) {
                if (S.notBlank(arg)) {
                    commandLine.add(arg);
                }
            }
        } else if (test) {
            commandLine.add("-Dprofile=test");
        }
        if (test) {
            commandLine.add("-Dtest.run=true");
        }
        commandLine.add("-cp");
        commandLine.add(classpath);
        commandLine.add(mainClass);

        return new ProcessExecutor(commandLine)
                .directory(mavenBaseDir)
                .destroyOnExit()
                .redirectErrorStream(true)
                .redirectOutput(this.outputStream);
    }
}
