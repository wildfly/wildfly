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
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.process.CommandLineConstants;

/**
 * Command line arguments for standalone mode.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
enum CommandLineArgument {

    ADMIN_ONLY {
        @Override
        public String argument() {
            return CommandLineConstants.ADMIN_ONLY;
        }

        @Override
        public String argumentExample() {
            return argument();
        }

        @Override
        public String instructions() {
            return MESSAGES.argAdminOnly();
        }
    },
    PUBLIC_BIND_ADDRESS {
        @Override
        public String argument() {
            return CommandLineConstants.PUBLIC_BIND_ADDRESS;
        }

        @Override
        public String argumentExample() {
            return String.format("%s=<value>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argPublicBindAddress();
        }
    },
    LEGACY_PUBLIC_BIND_ADDRESS {
        @Override
        public String argument() {
            return CommandLineConstants.PUBLIC_BIND_ADDRESS;
        }

        @Override
        public String argumentExample() {
            return String.format("%s <value>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argPublicBindAddress();
        }
    },
    INTERFACE_BIND_ADDRESS {
        @Override
        public String argument() {
            return CommandLineConstants.PUBLIC_BIND_ADDRESS;
        }

        @Override
        public String argumentExample() {
            return String.format("%s<interface>=<value>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argInterfaceBindAddress();
        }
    },
    SHORT_SERVER_CONFIG {
        @Override
        public String argument() {
            return CommandLineConstants.SHORT_SERVER_CONFIG;
        }

        @Override
        public String argumentExample() {
            return String.format("%s=<config>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argServerConfig();
        }
    },
    LEGACY_SHORT_SERVER_CONFIG {
        @Override
        public String argument() {
            return CommandLineConstants.SHORT_SERVER_CONFIG;
        }

        @Override
        public String argumentExample() {
            return String.format("%s <config>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argServerConfig();
        }
    },
    SYSTEM_PROPERTY {
        @Override
        public String argument() {
            return CommandLineConstants.SYS_PROP;
        }

        @Override
        public String argumentExample() {
            return String.format("%s<name>[=<value>]", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argSystem();
        }
    },
    SHORT_HELP {
        @Override
        public String argument() {
            return CommandLineConstants.SHORT_HELP;
        }

        @Override
        public String argumentExample() {
            return argument();
        }

        @Override
        public String instructions() {
            return MESSAGES.argHelp();
        }
    },
    HELP {
        @Override
        public String argument() {
            return CommandLineConstants.HELP;
        }

        @Override
        public String argumentExample() {
            return argument();
        }

        @Override
        public String instructions() {
            return MESSAGES.argHelp();
        }
    },
    SHORT_PROPERTIES {
        @Override
        public String argument() {
            return CommandLineConstants.SHORT_PROPERTIES;
        }

        @Override
        public String argumentExample() {
            return String.format("%s=<url>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argProperties();
        }
    },
    LEGACY_SHORT_PROPERTIES {
        @Override
        public String argument() {
            return CommandLineConstants.SHORT_PROPERTIES;
        }

        @Override
        public String argumentExample() {
            return String.format("%s <url>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argProperties();
        }
    },
    PROPERTIES {
        @Override
        public String argument() {
            return CommandLineConstants.PROPERTIES;
        }

        @Override
        public String argumentExample() {
            return String.format("%s=<url>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argProperties();
        }
    },
    SERVER_CONFIG {
        @Override
        public String argument() {
            return CommandLineConstants.SERVER_CONFIG;
        }

        @Override
        public String argumentExample() {
            return String.format("--%s=<config>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argServerConfig();
        }
    },
    DEFAULT_MULTICAST_ADDRESS {
        @Override
        public String argument() {
            return CommandLineConstants.DEFAULT_MULTICAST_ADDRESS;
        }

        @Override
        public String argumentExample() {
            return String.format("%s=<value>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argDefaultMulticastAddress();
        }
    },
    LEGACY_DEFAULT_MULTICAST_ADDRESS {
        @Override
        public String argument() {
            return CommandLineConstants.DEFAULT_MULTICAST_ADDRESS;
        }

        @Override
        public String argumentExample() {
            return String.format("%s <value>", argument());
        }

        @Override
        public String instructions() {
            return MESSAGES.argDefaultMulticastAddress();
        }
    },
    LEGACY_SHORT_VERSION {
        @Override
        public String argument() {
            return CommandLineConstants.OLD_SHORT_VERSION;
        }

        @Override
        public String argumentExample() {
            return argument();
        }

        @Override
        public String instructions() {
            return MESSAGES.argVersion();
        }
    },
    SHORT_VERSION {
        @Override
        public String argument() {
            return CommandLineConstants.SHORT_VERSION;
        }

        @Override
        public String argumentExample() {
            return argument();
        }

        @Override
        public String instructions() {
            return MESSAGES.argVersion();
        }
    },
    VERSION {
        @Override
        public String argument() {
            return CommandLineConstants.VERSION;
        }

        @Override
        public String argumentExample() {
            return argument();
        }

        @Override
        public String instructions() {
            return MESSAGES.argVersion();
        }
    };

    private static String USAGE;
    static final String NEW_LINE = String.format("%n");

    /**
     * The command line argument.
     *
     * @return the argument.
     */
    public abstract String argument();

    /**
     * An example of how the argument is used.
     *
     * @return the example.
     */
    public abstract String argumentExample();

    /**
     * The argument instructions.
     *
     * @return the instructions.
     */
    public abstract String instructions();

    @Override
    public String toString() {
        final List<String> instructions = new ArrayList<String>();
        segmentInstructions(instructions(), instructions);
        StringBuilder sb = new StringBuilder(String.format("    %-35s %s", argumentExample(), instructions.get(0)));
        for (int i = 1; i < instructions.size(); i++) {
            sb.append(NEW_LINE);
            sb.append(String.format("%-40s%s", " ", instructions.get(i)));
        }
        sb.append(NEW_LINE);
        return sb.toString();
    }

    private static void segmentInstructions(String instructions, List<String> segments) {
        if (instructions.length() <= 40) {
            segments.add(instructions);
        } else {
            String testFragment = instructions.substring(0, 40);
            int lastSpace = testFragment.lastIndexOf(' ');
            if (lastSpace < 0) {
                // degenerate case; we just have to chop not at a space
                lastSpace = 39;
            }
            segments.add(instructions.substring(0, lastSpace + 1));
            segmentInstructions(instructions.substring(lastSpace + 1), segments);
        }
    }

    public static void printUsage(final PrintStream out) {
        out.print(usage());
    }

    public static String usage() {
        if (USAGE == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append(MESSAGES.argUsage()).append(NEW_LINE);
            for (CommandLineArgument arg : CommandLineArgument.values()) {
                sb.append(arg.toString()).append(NEW_LINE);
            }
            USAGE = sb.toString();
        }
        return USAGE;
    }
}
