/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.appclient.subsystem;

import java.io.PrintStream;

import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.as.controller.persistence.ConfigurationExtensionFactory;
import org.jboss.as.process.CommandLineArgumentUsage;
import org.jboss.as.process.CommandLineConstants;

public class CommandLineArgumentUsageImpl extends CommandLineArgumentUsage {
    public static void init(){

        addArguments(CommandLineConstants.APPCLIENT_CONFIG + "=<config>");
        instructions.add(AppClientLogger.ROOT_LOGGER.argAppClientConfig());

        addArguments( CommandLineConstants.SHORT_HELP, CommandLineConstants.HELP);
        instructions.add(AppClientLogger.ROOT_LOGGER.argHelp());

        addArguments(CommandLineConstants.HOST + "=<url>", CommandLineConstants.SHORT_HOST + "=<url>");
        instructions.add(AppClientLogger.ROOT_LOGGER.argHost());

        addArguments(CommandLineConstants.SHORT_PROPERTIES + "=<url>", CommandLineConstants.PROPERTIES + "=<url>");
        instructions.add(AppClientLogger.ROOT_LOGGER.argProperties());

        addArguments(CommandLineConstants.CONNECTION_PROPERTIES + "=<url>");
        instructions.add(AppClientLogger.ROOT_LOGGER.argConnectionProperties());

        addArguments(CommandLineConstants.SYS_PROP + "<name>[=value]");
        instructions.add(AppClientLogger.ROOT_LOGGER.argSystemProperty());

        addArguments(CommandLineConstants.SHORT_VERSION, CommandLineConstants.VERSION);
        instructions.add(AppClientLogger.ROOT_LOGGER.argVersion());

        addArguments(CommandLineConstants.SECMGR);
        instructions.add(AppClientLogger.ROOT_LOGGER.argSecMgr());

        if(ConfigurationExtensionFactory.isConfigurationExtensionSupported()) {
            addArguments(ConfigurationExtensionFactory.getCommandLineUsageArguments());
            instructions.add(ConfigurationExtensionFactory.getCommandLineInstructions());
        }
    }

    public static void printUsage(final PrintStream out) {
        init();
        out.print(AppClientLogger.ROOT_LOGGER.usageDescription());
        out.print(usage("appclient"));
    }
}
