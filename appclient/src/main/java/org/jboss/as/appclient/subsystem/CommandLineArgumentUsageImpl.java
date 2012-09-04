package org.jboss.as.appclient.subsystem;

import static org.jboss.as.appclient.logging.AppClientMessages.MESSAGES;

import java.io.PrintStream;

import org.jboss.as.process.CommandLineArgumentUsage;
import org.jboss.as.process.CommandLineConstants;

public class CommandLineArgumentUsageImpl extends CommandLineArgumentUsage {
    public static void init(){

        addArguments(CommandLineConstants.APPCLIENT_CONFIG + "=<config>");
        instructions.add(MESSAGES.argAppClientConfig());

        addArguments( CommandLineConstants.SHORT_HELP, CommandLineConstants.HELP);
        instructions.add(MESSAGES.argHelp());

        addArguments(CommandLineConstants.HOST + "=<url>", CommandLineConstants.SHORT_HOST + "=<url>");
        instructions.add(MESSAGES.argHost());

        addArguments(CommandLineConstants.SHORT_PROPERTIES + "=<url>", CommandLineConstants.PROPERTIES + "=<url>");
        instructions.add(MESSAGES.argProperties());

        addArguments(CommandLineConstants.CONNECTION_PROPERTIES + "=<url>");
        instructions.add(MESSAGES.argConnectionProperties());

        addArguments(CommandLineConstants.SYS_PROP + "<name>[=value]");
        instructions.add(MESSAGES.argSystemProperty());

        addArguments(CommandLineConstants.SHORT_VERSION, CommandLineConstants.VERSION);
        instructions.add(MESSAGES.argVersion());
    }

    public static void printUsage(final PrintStream out) {
        init();
        out.print(usage());
    }
}
