/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.management.cli;

import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.wildfly.core.launcher.CliCommandBuilder;
import org.wildfly.core.launcher.Launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;

/**
 * Creates the CLI Process
 *
 * Deliberately made abstract and package-private.
 * This was created to isolate this code from the
 * CliProcessWrapper code, but not to be used independently.
 *
 * Created by joe on 6/9/15.
 */
abstract class CliProcessBuilder{

    protected CliProcessWrapper cliProcessWrapper;
    private CliCommandBuilder cliCommandBuilder;
    private String cliConfigFile;

    /**
     * Creates the CLI Process launched as a modular application or as non-modular application, in which case the CLI process
     * is launched by using jboss-cli-client.jar
     *
     * @param modular Whether the process will be launched as a modular application.
     */
    public CliProcessBuilder(boolean modular){
        String jbossDist = TestSuiteEnvironment.getSystemProperty("jboss.dist");
        if (modular) {
            cliCommandBuilder = CliCommandBuilder.asModularLauncher(jbossDist);
            cliCommandBuilder.addJavaOptions(System.getProperty("cli.jvm.args", "").split("\\s+"));
        } else {
            cliCommandBuilder = CliCommandBuilder.asJarLauncher(jbossDist);
            cliCommandBuilder.addJavaOptions(System.getProperty("cli.jvm.args.non-modular", "").split("\\s+"));
        }
    }

    public CliProcessWrapper addCliArgument(String argument){
        cliCommandBuilder.addCliArgument(argument);
        return cliProcessWrapper;
    }

    public CliProcessWrapper addJavaOption(String argument){
        cliCommandBuilder.addJavaOption(argument);
        return cliProcessWrapper;
    }

    public CliProcessWrapper setCliConfig(String cliConfigFile){
        this.cliConfigFile = cliConfigFile;
        return cliProcessWrapper;
    }

    public Process createProcess() {
        String jbossDist = TestSuiteEnvironment.getSystemProperty("jboss.dist");

        if(jbossDist==null) {
            fail("jboss.dist system property is not set");
        }
        String modulePath = TestSuiteEnvironment.getSystemProperty("module.path");
        if(modulePath==null) {
            modulePath = jbossDist + File.separator + "modules";
        }

        cliCommandBuilder.addModuleDirs(modulePath.split(Pattern.quote(File.pathSeparator)));

        // Useful for debugging single tests. Added to the process command, it causes
        // the process to attach to a waiting debugger listening on the specified port.
        // If uncommented during normal testing, causes all sorts of failures.
        //cliCommandBuilder.addJavaOption("-agentlib:jdwp=transport=dt_socket,server=n,address=localhost.localdomain:8765,suspend=y");

        if (cliConfigFile != null) {
            cliCommandBuilder.addJavaOption("-Djboss.cli.config=" + cliConfigFile);
        } else {
            cliCommandBuilder.addJavaOption("-Djboss.cli.config=" + Paths.get(jbossDist, "bin", "jboss-cli.xml"));
        }

        try {
            return Launcher.of(cliCommandBuilder).launch();
        } catch (IOException e) {
            fail("Failed to start CLI process: " + e.getLocalizedMessage());
        }

        return null;
    }

}
