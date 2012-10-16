/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server;

import static org.jboss.as.server.ServerMessages.MESSAGES;

import java.io.PrintStream;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.version.Usage;

public class CommandLineUsage {

    private static Usage getUsage() {

        final Usage usage = new Usage();

        usage.addArguments(CommandLineConstants.ADMIN_ONLY);
        usage.addInstruction(MESSAGES.argAdminOnly());

        usage.addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + " <value>", CommandLineConstants.PUBLIC_BIND_ADDRESS + "=<value>");
        usage.addInstruction(MESSAGES.argPublicBindAddress());

        usage.addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + "<interface>=<value>");
        usage.addInstruction(MESSAGES.argInterfaceBindAddress());

        usage.addArguments(CommandLineConstants.SHORT_SERVER_CONFIG + " <config>", CommandLineConstants.SHORT_SERVER_CONFIG + "=<config>");
        usage.addInstruction(MESSAGES.argShortServerConfig());

        usage.addArguments(CommandLineConstants.DEBUG + " [<port>]");
        usage.addInstruction(MESSAGES.argDebugPort());

        usage.addArguments(CommandLineConstants.SYS_PROP + "<name>[=<value>]");
        usage.addInstruction(MESSAGES.argSystem());

        usage.addArguments(CommandLineConstants.SHORT_HELP, CommandLineConstants.HELP);
        usage.addInstruction(MESSAGES.argHelp());

        usage.addArguments(CommandLineConstants.READ_ONLY_SERVER_CONFIG + "=<config>");
        usage.addInstruction(MESSAGES.argReadOnlyServerConfig());

        usage.addArguments(CommandLineConstants.SHORT_PROPERTIES + " <url>", CommandLineConstants.SHORT_PROPERTIES + "=<url>", CommandLineConstants.PROPERTIES + "=<url>");
        usage.addInstruction(MESSAGES.argProperties());

        usage.addArguments(CommandLineConstants.SECURITY_PROP + "<name>[=<value>]");
        usage.addInstruction(MESSAGES.argSecurityProperty());

        usage.addArguments(CommandLineConstants.SERVER_CONFIG + "=<config>");
        usage.addInstruction(MESSAGES.argServerConfig());

        usage.addArguments(CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + " <value>", CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + "=<value>");
        usage.addInstruction(MESSAGES.argDefaultMulticastAddress());

        usage.addArguments(CommandLineConstants.SHORT_VERSION, CommandLineConstants.OLD_SHORT_VERSION, CommandLineConstants.VERSION);
        usage.addInstruction(MESSAGES.argVersion());

        return usage;
    }

    public static void printUsage(final PrintStream out) {
        final Usage usage = getUsage();
        final String headline = usage.getDefaultUsageHeadline("standalone");
        out.print(usage.usage(headline));
    }

}
