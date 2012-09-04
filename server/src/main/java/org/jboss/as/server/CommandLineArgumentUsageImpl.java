package org.jboss.as.server;

import static org.jboss.as.server.ServerMessages.MESSAGES;

import java.io.PrintStream;

import org.jboss.as.process.CommandLineArgumentUsage;
import org.jboss.as.process.CommandLineConstants;

public class CommandLineArgumentUsageImpl extends CommandLineArgumentUsage {

    public static void init(){

        addArguments(CommandLineConstants.ADMIN_ONLY);
        instructions.add(MESSAGES.argAdminOnly());

        addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + " <value>", CommandLineConstants.PUBLIC_BIND_ADDRESS + "=<value>");
        instructions.add(MESSAGES.argPublicBindAddress());

        addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + "<interface>=<value>");
        instructions.add(MESSAGES.argInterfaceBindAddress());

        addArguments(CommandLineConstants.SHORT_SERVER_CONFIG + " <config>", CommandLineConstants.SHORT_SERVER_CONFIG + "=<config>");
        instructions.add(MESSAGES.argServerConfig());

        addArguments(CommandLineConstants.SYS_PROP + "<name>[=<value>]");
        instructions.add(MESSAGES.argSystem());

        addArguments(CommandLineConstants.SHORT_HELP, CommandLineConstants.HELP);
        instructions.add(MESSAGES.argHelp());

        addArguments(CommandLineConstants.READ_ONLY_SERVER_CONFIG + "=<config>");
        instructions.add(MESSAGES.argReadOnlyServerConfig());

        addArguments(CommandLineConstants.SHORT_PROPERTIES + " <url>", CommandLineConstants.SHORT_PROPERTIES + "=<url>", CommandLineConstants.PROPERTIES + "=<url>");
        instructions.add(MESSAGES.argProperties() );

        addArguments(CommandLineConstants.SECURITY_PROP + "<name>[=<value>]");
        instructions.add(MESSAGES.argSecurityProperty());

        addArguments(CommandLineConstants.SERVER_CONFIG + "=<config>");
        instructions.add(MESSAGES.argServerConfig());

        addArguments( CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + " <value>", CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + "=<value>");
        instructions.add(MESSAGES.argDefaultMulticastAddress());

        addArguments(CommandLineConstants.SHORT_VERSION, CommandLineConstants.OLD_SHORT_VERSION, CommandLineConstants.VERSION);
        instructions.add(MESSAGES.argVersion());

    }

    public static void printUsage(final PrintStream out) {
        init();
        out.print(usage());
    }

}
