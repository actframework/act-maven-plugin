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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.osgl.$;
import org.osgl.util.S;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a refactored version of
 * NinjaRunMojo.java from the Ninja Web Framework
 * <p>
 * Original source code can be found here:
 * https://github.com/ninjaframework/ninja/blob/develop/ninja-maven-plugin/src/main/java/ninja/maven/NinjaRunMojo.java
 */
@Mojo(name = "run",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.NONE,
        threadSafe = true)
public class ActMojo extends AbstractMojo {

    @Parameter(property = "act.lombok", defaultValue = "${app.lombok}", required = false, readonly = true)
    protected String lombok;

    @Parameter(property = "act.appEntry", defaultValue = "${app.entry}", required = true, readonly = true)
    protected String appEntry;

    @Parameter(property = "act.skip", defaultValue = "false", required = true)
    protected boolean skip;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "act.jpdaPort", defaultValue = "5005", required = true)
    private int jpdaPort;

    @Parameter(property = "act.jvmArgs", required = false)
    private String jvmArgs;

    @Parameter(property = "act.outputDirectory", defaultValue = "${project.build.outputDirectory}", required = true)
    private String buildOutputDirectory;

    @Parameter(property = "act.useDefaultExcludes", defaultValue = "true", required = true)
    protected boolean useDefaultExcludes;

    @Parameter(property = "act.settleDownMillis", defaultValue = "500", required = false)
    private Long settleDownMillis;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skip flag is on. Will not execute.");
            return;
        }

        if (test()) {
            String mavenTestSkip = System.getProperty("maven.test.skip");
            if (S.notBlank(mavenTestSkip) && $.bool(mavenTestSkip)) {
                return;
            }
        }

        // make sure we are good when building act-starter-parent project
        if ("to.be.Overwritten".equals(appEntry) || "ToBeOverwritten".equals(appEntry)) {
            getLog().info("Missing AppEntry. Will not execute.");
            return;
        }

        checkClasses(buildOutputDirectory);

        List<String> classpathItems = new ArrayList<>();
        classpathItems.add(buildOutputDirectory);

        for (Artifact artifact : project.getArtifacts()) {
            classpathItems.add(artifact.getFile().toString()); //NOSONAR
        }

        startRunner(classpathItems);
    }

    private void startRunner(List<String> classpathItems) throws MojoExecutionException {
        Runner machine = createRunner(classpathItems);
        machine.start();
    }

    protected Runner createRunner(List<String> classpathItems) {
        mergeSystemProperties();
        return new Runner(appEntry, StringUtils.join(classpathItems, File.pathSeparator), project.getBasedir(), jpdaPort, jvmArgs, test(), lombok);
    }

    private void mergeSystemProperties() {
        _mergeSysProp("test.run");
        _mergeSysProp("act.test.run");

        _mergeSysProp("e2e.run");
        _mergeSysProp("act.e2e.run");

        _mergeSysProp("profile");
        _mergeSysProp("act.profile");
    }

    private void _mergeSysProp(String key) {
        if (null == jvmArgs) {
            jvmArgs = "";
        }
        String s = System.getProperty(key);
        if (S.notBlank(s)) {
            if (key.startsWith("act.")) {
                key = key.substring(4);
            }
            jvmArgs = S.concat(jvmArgs, " ", "-D", key, "=", s);
        }
    }

    protected boolean test() {
        return false;
    }

    @SuppressWarnings("all")
    public void checkClasses(String classesDirectory) {
        if (!new File(classesDirectory).exists()) {
            // TODO: call maven compiler to compile
            getLog().error("Directory with classes does not exist: " + classesDirectory);
            System.exit(1); //NOSONAR
        }
    }
}
