/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.prospero.cli;

import org.wildfly.prospero.cli.CliConsole;
import org.wildfly.prospero.cli.CliMain;
import picocli.CommandLine;

/**
 * Main class that does the same as {@link org.wildfly.prospero.cli.CliMain} but with no System.exit() calls.
 * This allows it to be used in maven build executions without terminating the build.
 */
public class ProsperoCliWrapper {

    public static void main(String[] args) throws Exception {
        CliConsole console = new CliConsole();
        CommandLine commandLine = CliMain.createCommandLine(console, args);
        int exit = commandLine.execute(args);
        if (exit != 0) {
            throw new RuntimeException("Unexpected exit code " + exit + " from CommandLine.execute");
        }
    }
}
