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
        instructions.add(MESSAGES.argShortServerConfig());

        addArguments(CommandLineConstants.DEBUG + " [<port>]");
        instructions.add(MESSAGES.argDebugPort());

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
        out.print(usage("standalone"));
    }

}
