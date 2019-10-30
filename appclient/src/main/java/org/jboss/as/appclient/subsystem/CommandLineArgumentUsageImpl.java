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

package org.jboss.as.appclient.subsystem;

import java.io.PrintStream;

import org.jboss.as.appclient.logging.AppClientLogger;
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
    }

    public static void printUsage(final PrintStream out) {
        init();
        out.print(AppClientLogger.ROOT_LOGGER.usageDescription());
        out.print(usage("appclient"));
    }
}
