/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.process;

import java.io.PrintStream;

import org.jboss.as.process.logging.ProcessLogger;

public class CommandLineArgumentUsageImpl extends CommandLineArgumentUsage {

    public static void init(){

        addArguments(CommandLineConstants.ADMIN_ONLY);
        instructions.add(ProcessLogger.ROOT_LOGGER.argAdminOnly());

        addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + " <value>", CommandLineConstants.PUBLIC_BIND_ADDRESS + "=<value>" );
        instructions.add(ProcessLogger.ROOT_LOGGER.argPublicBindAddress());

        addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + "<interface>=<value>" );
        instructions.add(ProcessLogger.ROOT_LOGGER.argInterfaceBindAddress());

        addArguments(CommandLineConstants.BACKUP_DC);
        instructions.add(ProcessLogger.ROOT_LOGGER.argBackup());

        addArguments(CommandLineConstants.SHORT_DOMAIN_CONFIG + " <config>", CommandLineConstants.SHORT_DOMAIN_CONFIG + "=<config>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argShortDomainConfig());

        addArguments(CommandLineConstants.CACHED_DC);
        instructions.add(ProcessLogger.ROOT_LOGGER.argCachedDc());

        addArguments(CommandLineConstants.SYS_PROP + "<name>[=<value>]");
        instructions.add(ProcessLogger.ROOT_LOGGER.argSystem());

        addArguments(CommandLineConstants.DOMAIN_CONFIG + "=<config>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argDomainConfig());

        addArguments(CommandLineConstants.SHORT_HELP, CommandLineConstants.HELP);
        instructions.add(ProcessLogger.ROOT_LOGGER.argHelp());

        addArguments(CommandLineConstants.HOST_CONFIG + "=<config>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argHostConfig());

        addArguments(CommandLineConstants.INTERPROCESS_HC_ADDRESS + "=<address>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argInterProcessHcAddress());

        addArguments(CommandLineConstants.INTERPROCESS_HC_PORT + "=<port>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argInterProcessHcPort());

        addArguments(CommandLineConstants.MASTER_ADDRESS +"=<address>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argMasterAddress());

        addArguments(CommandLineConstants.MASTER_PORT + "=<port>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argMasterPort());

        addArguments(CommandLineConstants.READ_ONLY_DOMAIN_CONFIG + "=<config>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argReadOnlyDomainConfig());

        addArguments(CommandLineConstants.READ_ONLY_HOST_CONFIG + "=<config>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argReadOnlyHostConfig());

        addArguments(CommandLineConstants.SHORT_PROPERTIES + " <url>", CommandLineConstants.SHORT_PROPERTIES + "=<url>", CommandLineConstants.PROPERTIES + "=<url>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argProperties());

        addArguments(CommandLineConstants.PROCESS_CONTROLLER_BIND_ADDR + "=<address>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argPcAddress());

        addArguments(CommandLineConstants.PROCESS_CONTROLLER_BIND_PORT + "=<port>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argPcPort());

        addArguments(CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + " <value>", CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + "=<value>");
        instructions.add(ProcessLogger.ROOT_LOGGER.argDefaultMulticastAddress());

        addArguments(CommandLineConstants.OLD_SHORT_VERSION, CommandLineConstants.SHORT_VERSION, CommandLineConstants.VERSION);
        instructions.add(ProcessLogger.ROOT_LOGGER.argVersion());
    }

    public static void printUsage(final PrintStream out) {
        init();
        out.print(usage("domain"));
    }
}
