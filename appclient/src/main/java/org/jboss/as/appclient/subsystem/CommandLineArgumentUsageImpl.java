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

package org.jboss.as.appclient.subsystem;

import static org.jboss.as.appclient.logging.AppClientMessages.MESSAGES;

import java.io.PrintStream;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.version.Usage;

public class CommandLineArgumentUsageImpl {

    private static Usage getUsage() {
        final Usage usage = new Usage();
        usage.addArguments(CommandLineConstants.APPCLIENT_CONFIG + "=<config>");
        usage.addInstruction(MESSAGES.argAppClientConfig());

        usage.addArguments(CommandLineConstants.SHORT_HELP, CommandLineConstants.HELP);
        usage.addInstruction(MESSAGES.argHelp());

        usage.addArguments(CommandLineConstants.HOST + "=<url>", CommandLineConstants.SHORT_HOST + "=<url>");
        usage.addInstruction(MESSAGES.argHost());

        usage.addArguments(CommandLineConstants.SHORT_PROPERTIES + "=<url>", CommandLineConstants.PROPERTIES + "=<url>");
        usage.addInstruction(MESSAGES.argProperties());

        usage.addArguments(CommandLineConstants.CONNECTION_PROPERTIES + "=<url>");
        usage.addInstruction(MESSAGES.argConnectionProperties());

        usage.addArguments(CommandLineConstants.SYS_PROP + "<name>[=value]");
        usage.addInstruction(MESSAGES.argSystemProperty());

        usage.addArguments(CommandLineConstants.SHORT_VERSION, CommandLineConstants.VERSION);
        usage.addInstruction(MESSAGES.argVersion());

        return usage;
    }

    public static void printUsage(final PrintStream out) {
        final Usage usage = getUsage();
        final String headline = usage.getDefaultUsageHeadline("appclient");
        out.print(usage.usage(headline));
    }
}
