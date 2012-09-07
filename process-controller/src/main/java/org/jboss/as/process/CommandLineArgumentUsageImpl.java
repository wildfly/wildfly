package org.jboss.as.process;

import static org.jboss.as.process.ProcessMessages.MESSAGES;

import java.io.PrintStream;

public class CommandLineArgumentUsageImpl extends CommandLineArgumentUsage {

    public static void init(){

        addArguments(CommandLineConstants.ADMIN_ONLY);
        instructions.add(MESSAGES.argAdminOnly());

        addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + " <value>", CommandLineConstants.PUBLIC_BIND_ADDRESS + "=<value>" );
        instructions.add(MESSAGES.argPublicBindAddress());

        addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + "<interface>=<value>" );
        instructions.add(MESSAGES.argInterfaceBindAddress());

        addArguments(CommandLineConstants.BACKUP_DC);
        instructions.add(MESSAGES.argBackup());

        addArguments(CommandLineConstants.SHORT_DOMAIN_CONFIG + " <config>", CommandLineConstants.SHORT_DOMAIN_CONFIG + "=<config>");
        instructions.add(MESSAGES.argShortDomainConfig());

        addArguments(CommandLineConstants.CACHED_DC);
        instructions.add(MESSAGES.argCachedDc());

        addArguments(CommandLineConstants.SYS_PROP + "<name>[=<value>]");
        instructions.add(MESSAGES.argSystem());

        addArguments(CommandLineConstants.DOMAIN_CONFIG + "=<config>");
        instructions.add(MESSAGES.argDomainConfig());

        addArguments(CommandLineConstants.SHORT_HELP, CommandLineConstants.HELP);
        instructions.add(MESSAGES.argHelp());

        addArguments(CommandLineConstants.HOST_CONFIG + "=<config>");
        instructions.add(MESSAGES.argHostConfig());

        addArguments(CommandLineConstants.INTERPROCESS_HC_ADDRESS + "=<address>");
        instructions.add(MESSAGES.argInterProcessHcAddress());

        addArguments(CommandLineConstants.INTERPROCESS_HC_PORT + "=<port>");
        instructions.add(MESSAGES.argInterProcessHcPort());

        addArguments(CommandLineConstants.MASTER_ADDRESS +"=<address>");
        instructions.add(MESSAGES.argMasterAddress());

        addArguments(CommandLineConstants.MASTER_PORT + "=<port>");
        instructions.add(MESSAGES.argMasterPort());

        addArguments(CommandLineConstants.READ_ONLY_DOMAIN_CONFIG + "=<config>");
        instructions.add(MESSAGES.argReadOnlyDomainConfig());

        addArguments(CommandLineConstants.READ_ONLY_HOST_CONFIG + "=<config>");
        instructions.add(MESSAGES.argReadOnlyHostConfig());

        addArguments(CommandLineConstants.SHORT_PROPERTIES + " <url>", CommandLineConstants.SHORT_PROPERTIES + "=<url>", CommandLineConstants.PROPERTIES + "=<url>");
        instructions.add(MESSAGES.argProperties());

        addArguments(CommandLineConstants.PROCESS_CONTROLLER_BIND_ADDR + "=<address>");
        instructions.add(MESSAGES.argPcAddress());

        addArguments(CommandLineConstants.PROCESS_CONTROLLER_BIND_PORT + "=<port>");
        instructions.add(MESSAGES.argPcPort());

        addArguments(CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + " <value>", CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + "=<value>");
        instructions.add(MESSAGES.argDefaultMulticastAddress());

        addArguments(CommandLineConstants.OLD_SHORT_VERSION, CommandLineConstants.SHORT_VERSION, CommandLineConstants.VERSION);
        instructions.add(MESSAGES.argVersion());
    }

    public static void printUsage(final PrintStream out) {
        init();
        out.print(usage());
    }
}
