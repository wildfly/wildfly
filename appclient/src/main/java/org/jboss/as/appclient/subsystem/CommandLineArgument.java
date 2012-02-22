/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.process.CommandLineConstants;

import static org.jboss.as.appclient.logging.AppClientMessages.MESSAGES;

/**
 * Date: 26.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
enum CommandLineArgument {

    APPCLIENT_CONFIG(CommandLineConstants.APPCLIENT_CONFIG, "=<config> ") {
        @Override
        public String instructions() {
            return MESSAGES.argAppClientConfig();
        }
    },

    HELP(CommandLineConstants.HELP, null, CommandLineConstants.SHORT_HELP) {
        @Override
        public String instructions() {
            return MESSAGES.argHelp();
        }
    },

    HOST(CommandLineConstants.HOST, "=<url>", CommandLineConstants.SHORT_HOST) {
        @Override
        public String instructions() {
            return MESSAGES.argHost();
        }
    },

    PROPERTIES(CommandLineConstants.PROPERTIES, "=<url>", CommandLineConstants.SHORT_PROPERTIES) {
        @Override
        public String instructions() {
            return MESSAGES.argProperties();
        }
    },
    CONNECTION_PROPERTIES(CommandLineConstants.CONNECTION_PROPERTIES, "=<url>") {
        @Override
        public String instructions() {
            return MESSAGES.connectionProperties();
        }
    },

    SYS_PROP(CommandLineConstants.SYS_PROP, "<name>[=value]") {
        @Override
        public String instructions() {
            return MESSAGES.argSystemProperty();
        }
    },

    UNKNOWN("unknown", null) {
        @Override
        public String instructions() {
            return "Unknown argument.";
        }
    },

    VERSION(CommandLineConstants.VERSION, null, CommandLineConstants.SHORT_VERSION) {
        @Override
        public String instructions() {
            return MESSAGES.argVersion();
        }
    };

    private static final String USAGE;

    static {
        final StringBuilder sb = new StringBuilder();
        sb.append(MESSAGES.argUsage());
        for (CommandLineArgument arg : CommandLineArgument.values()) {
            if (arg == UNKNOWN) {
                continue;
            }
            sb.append(arg.toString());
        }
        USAGE = sb.toString();
    }

    private final String argument;
    private final String exampleDesc;
    private final String[] altArguments;

    CommandLineArgument(final String argument, final String exampleDesc, final String... altArguments) {
        this.argument = argument;
        this.exampleDesc = exampleDesc;
        this.altArguments = altArguments;
    }

    /**
     * The command line argument.
     *
     * @return the argument.
     */
    public String argument() {
        return argument;
    }

    public String[] altArguments() {
        return altArguments;
    }

    private String argumentExample(final String arg) {
        if (exampleDesc == null) {
            return arg;
        }
        return String.format("%s%s", arg, exampleDesc);
    }

    /**
     * The argument instructions.
     *
     * @return the instructions.
     */
    public abstract String instructions();

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(String.format("    %-35s %s%n", argumentExample(argument), instructions()));
        if (altArguments != null && altArguments.length > 0) {
            for (String arg : altArguments) {
                result.append(String.format("%n    %-35s %s%n", argumentExample(arg), instructions()));
            }
        }

        return result.toString();
    }

    public static CommandLineArgument forArg(final String arg) {
        for (CommandLineArgument argument : values()) {
            if (arg.startsWith(argument.argument)) {
                return argument;
            }
            for (String sArg : argument.altArguments) {
                if (arg.startsWith(sArg)) {
                    return argument;
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * Prints the usage to the print stream.
     *
     * @param out the print stream to print the usage to.
     */
    public static void printUsage(final PrintStream out) {
        out.print(USAGE);
    }

    /**
     * Returns a string with the of the usage.
     *
     * @return the usage.
     */
    public static String usage() {
        return USAGE;
    }
}
