/**
 * Copyright (C) 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
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
 * Original source code can be found here:
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
    private final boolean e2e;

    public Runner(String mainClass, String classpath, File mavenBaseDir, int jpdaPort, String jvmArgs, boolean e2e) {
        this.outputStream = System.out; //NOSONAR
        this.mainClass = mainClass;
        this.classpath = classpath;
        this.mavenBaseDir = mavenBaseDir;
        this.jpdaPort = jpdaPort;
        this.jvmArgs = jvmArgs;
        this.e2e = e2e;
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

    public void start() {
        synchronized (this) {
            try {
                if (this.startedProcess != null) {
                    this.startedProcess.getProcess().destroy();
                    this.startedProcess.getFuture().get();
                }
                this.startedProcess = startProcess();
                this.startedProcess.getProcess().waitFor();
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
        if (jpdaPort > 0) {
            LOG.info("Listening for jpda connection at " + jpdaPort);
            commandLine.add("-Xdebug");
            commandLine.add(String.format("-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=%s", jpdaPort));
        }
        if (StringUtils.isNotBlank(jvmArgs)) {
            List<String> args = S.fastSplit(jvmArgs, " ");
            for (String arg : args) {
                if (S.notBlank(arg)) {
                    commandLine.add(arg);
                }
            }
        }
        if (e2e) {
            commandLine.add("-Dprofile=e2e");
        } else {
            commandLine.add("-Dapp.mode=dev");
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
