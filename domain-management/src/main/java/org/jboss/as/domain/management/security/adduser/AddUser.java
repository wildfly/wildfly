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

package org.jboss.as.domain.management.security.adduser;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * A command line utility to add new users to the mgmt-users.properties files.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:g.grossetie@gmail.com">Guillaume Grossetie</a>
 */
public class AddUser {

    private static final String JBOSS_HOME_ENV = "JBOSS_HOME";

    public static final String SERVER_BASE_DIR = "jboss.server.base.dir";
    public static final String SERVER_CONFIG_DIR = "jboss.server.config.dir";
    public static final String SERVER_CONFIG_USER_DIR = "jboss.server.config.user.dir";
    public static final String DOMAIN_BASE_DIR = "jboss.domain.base.dir";
    public static final String DOMAIN_CONFIG_DIR = "jboss.domain.config.dir";
    public static final String DOMAIN_CONFIG_USER_DIR = "jboss.domain.config.user.dir";

    public static final String DEFAULT_MANAGEMENT_REALM = "ManagementRealm";
    public static final String DEFAULT_APPLICATION_REALM = "ApplicationRealm";
    public static final String MGMT_USERS_PROPERTIES = "mgmt-users.properties";
    public static final String MGMT_GROUPS_PROPERTIES = "mgmt-groups.properties";
    public static final String APPLICATION_USERS_PROPERTIES = "application-users.properties";
    public static final String APPLICATION_ROLES_PROPERTIES = "application-roles.properties";

    public static final String NEW_LINE = String.format("%n");
    public static final String SPACE = " ";
    private static final Properties argsCliProps = new Properties();

    private final ConsoleWrapper theConsole;

    protected State nextState;

    protected AddUser(RuntimeOptions options, final String realm) {
        theConsole = options.getConsoleWrapper();
        StateValues stateValues = new StateValues(options);
        if (realm != null) {
            stateValues.setRealm(realm);
            stateValues.setRealmMode(RealmMode.USER_SUPPLIED);
        }

        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
        if (options.getUserProperties() != null || options.getGroupProperties() != null) {
            // If we have property files specified we do not need to check the running mode.
            nextState = new PropertyFileFinder(theConsole, stateValues);
        } else {
            nextState = new PropertyFilePrompt(theConsole, stateValues);
        }
    }

    private AddUser(RuntimeOptions options, final FileMode fileMode, final String user, final String password, final String realm, final RealmMode realmMode) {
        StateValues stateValues = new StateValues(options);

        final Interactiveness howInteractive;
        boolean silent = Boolean.valueOf(argsCliProps.getProperty(CommandLineArgument.SILENT.key()));
        if (silent) {
            howInteractive = Interactiveness.SILENT;
        } else {
            howInteractive = Interactiveness.NON_INTERACTIVE;
        }
        stateValues.setHowInteractive(howInteractive);

        // Silent modes still need to be able to output an error on failure.
        theConsole = options.getConsoleWrapper();
        if (theConsole.getConsole() == null && !howInteractive.equals(Interactiveness.SILENT)) {
            throw MESSAGES.noConsoleAvailable();
        }
        // Username should not be null or empty.
        if (user == null || user.isEmpty()) {
            nextState = new ErrorState(theConsole, MESSAGES.noUsernameExiting(), null, stateValues);
            return;
        }
        stateValues.setUserName(user);
        // Password should not be null or empty.
        if (password == null || password.isEmpty()) {
            nextState = new ErrorState(theConsole, MESSAGES.noPasswordExiting(), null, stateValues);
            return;
        }
        stateValues.setPassword(password.toCharArray());
        stateValues.setRealm(realm);
        stateValues.setRealmMode(realmMode);
        stateValues.setFileMode(fileMode);
        String groups = argsCliProps.getProperty(CommandLineArgument.GROUPS.key());
        if (groups == null) {
            groups = argsCliProps.getProperty(CommandLineArgument.ROLE.key());
        }
        stateValues.setGroups(groups);

        nextState = new PropertyFileFinder(theConsole, stateValues);
    }

    private AddUser(RuntimeOptions options, final FileMode fileMode, final String user, final String password) {
        this(options, fileMode, user, password, fileMode == FileMode.MANAGEMENT ? DEFAULT_MANAGEMENT_REALM : DEFAULT_APPLICATION_REALM, RealmMode.DEFAULT);
    }

    protected void run() {
        while ((nextState = nextState.execute()) != null) {
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        FileMode fileMode = FileMode.MANAGEMENT;
        RuntimeOptions options = new RuntimeOptions();
        options.setConsoleWrapper(new JavaConsole());
        options.setJBossHome(System.getenv(JBOSS_HOME_ENV));

        if (args.length >= 1) {

            Iterator<String> it = Arrays.asList(args).iterator();
            String temp;
            while (it.hasNext()) {
                temp = it.next();
                if (CommandLineArgument.HELP.match(temp)) {
                    usage(options.getConsoleWrapper());
                    return;
                }
                if (CommandLineArgument.DOMAIN_CONFIG_DIR_USERS.match(temp)) {
                    options.setDomainConfigDir(it.next());
                } else if (CommandLineArgument.SERVER_CONFIG_DIR_USERS.match(temp)) {
                    options.setServerConfigDir(it.next());
                } else if (CommandLineArgument.APPLICATION_USERS.match(temp)) {
                    fileMode = FileMode.APPLICATION;
                } else if (CommandLineArgument.USER_PROPERTIES.match(temp)) {
                    options.setUserProperties(it.next());
                } else if (CommandLineArgument.GROUP_PROPERTIES.match(temp)) {
                    options.setGroupProperties(it.next());
                } else {
                    // Find the command-line option
                    CommandLineArgument commandLineArgument = findCommandLineOption(temp);
                    if (commandLineArgument != null) {
                        final String value;
                        if (CommandLineArgument.SILENT.equals(commandLineArgument)) {
                            value = Boolean.TRUE.toString();
                        } else {
                            value = it.hasNext() ? it.next() : null;
                        }
                        if (value != null) {
                            argsCliProps.setProperty(commandLineArgument.key(), value);
                        }
                    } else {
                        // By default, the first arg without option is the username,
                        final String userKey = CommandLineArgument.USER.key();
                        if (!argsCliProps.containsKey(userKey)) {
                            argsCliProps.setProperty(userKey, temp);
                        }
                        // the second arg is the password and,
                        else {
                            final String passwordKey = CommandLineArgument.PASSWORD.key();
                            if (!argsCliProps.containsKey(passwordKey)) {
                                argsCliProps.setProperty(passwordKey, temp);
                            }
                            // the third one is the realm.
                            else {
                                final String realmKey = CommandLineArgument.REALM.key();
                                if (!argsCliProps.containsKey(realmKey)) {
                                    argsCliProps.setProperty(realmKey, temp);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (argsCliProps.containsKey(CommandLineArgument.PASSWORD.key()) || argsCliProps.containsKey(CommandLineArgument.USER.key())) {
            final String password = argsCliProps.getProperty(CommandLineArgument.PASSWORD.key());
            final String user = argsCliProps.getProperty(CommandLineArgument.USER.key());
            if (argsCliProps.containsKey(CommandLineArgument.REALM.key())) {
                new AddUser(options, fileMode, user, password, argsCliProps.getProperty(CommandLineArgument.REALM.key()), RealmMode.USER_SUPPLIED).run();
            } else {
                new AddUser(options, fileMode, user, password).run();
            }
        } else {
            String realm = null;
            if (argsCliProps.containsKey(CommandLineArgument.REALM.key())) {
                realm = argsCliProps.getProperty(CommandLineArgument.REALM.key());
            }
            new AddUser(options, realm).run();
        }
    }

    private static void usage(ConsoleWrapper consoleWrapper) {
        CommandLineArgument.printUsage(consoleWrapper);
    }

    public enum Interactiveness {
        SILENT, NON_INTERACTIVE, INTERACTIVE
    }

    public enum RealmMode {
        USER_SUPPLIED, DEFAULT, DISCOVERED
    }

    public enum FileMode {
        MANAGEMENT, APPLICATION, UNDEFINED
    }

    /**
     * Find the command-line arg corresponding to the parameter {@code arg}.
     *
     * @param arg
     * @return The corresponding arg or null.
     */
    private static CommandLineArgument findCommandLineOption(String arg) {
        for (CommandLineArgument commandLineArgument : CommandLineArgument.values()) {
            if (commandLineArgument.match(arg)) {
                return commandLineArgument;
            }
        }
        return null;
    }

    protected enum CommandLineArgument {

        APPLICATION_USERS("-a") {
            @Override
            public String instructions() {
                return MESSAGES.argApplicationUsers();
            }
        },
        DOMAIN_CONFIG_DIR_USERS("-dc") {
            @Override
            public String argumentExample() {
                return super.argumentExample().concat(" <value>");
            }

            @Override
            public String instructions() {
                return MESSAGES.argDomainConfigDirUsers();
            }
        },
        SERVER_CONFIG_DIR_USERS("-sc") {
            @Override
            public String argumentExample() {
                return super.argumentExample().concat(" <value>");
            }

            @Override
            public String instructions() {
                return MESSAGES.argServerConfigDirUsers();
            }
        },
        USER_PROPERTIES("-up", "--user-properties") {
            @Override
            public String argumentExample() {
                return super.argumentExample().concat(" <value>");
            }

            @Override
            public String instructions() {
                return MESSAGES.argUserProperties();
            }

        },
        GROUPS("-g", "--group") {
            @Override
            public String argumentExample() {
                return super.argumentExample().concat(" <value>");
            }

            @Override
            public String instructions() {
                return MESSAGES.argGroup();
            }
        },
        GROUP_PROPERTIES("-gp", "--group-properties") {
            @Override
            public String argumentExample() {
                return super.argumentExample().concat(" <value>");
            }

            @Override
            public String instructions() {
                return MESSAGES.argGroupProperties();
            }

        },
        PASSWORD("-p", "--password") {
            @Override
            public String argumentExample() {
                return super.argumentExample().concat(" <value>");
            }

            @Override
            public String instructions() {
                return MESSAGES.argPassword();
            }
        },
        USER("-u", "--user") {
            @Override
            public String argumentExample() {
                return super.argumentExample().concat(" <value>");
            }

            @Override
            public String instructions() {
                return MESSAGES.argUser();
            }
        },
        REALM("-r", "--realm") {
            @Override
            public String argumentExample() {
                return super.argumentExample().concat(" <value>");
            }

            @Override
            public String instructions() {
                return MESSAGES.argRealm();
            }
        },
        SILENT("-s", "--silent", "--silent=true") {
            @Override
            public String instructions() {
                return MESSAGES.argSilent();
            }
        },
        ROLE("-ro", "--role") {
            /*
             * Deprecated in favour of groups.
             */

            @Override
            public String argumentExample() {
                return super.argumentExample().concat(" <value>");
            }

            @Override
            public String instructions() {
                return MESSAGES.argRole();
            }
        },
        HELP("-h", "--help") {
            @Override
            public String instructions() {
                return MESSAGES.argHelp();
            }
        };

        private static String USAGE;

        private String shortArg;
        private String longArg;
        private String additionalArg;

        private CommandLineArgument(String option) {
            this.shortArg = option;
        }

        private CommandLineArgument(String shortArg, String longArg) {
            this.shortArg = shortArg;
            this.longArg = longArg;
        }

        private CommandLineArgument(String shortArg, String longArg, String additionalArg) {
            this.shortArg = shortArg;
            this.longArg = longArg;
            this.additionalArg = additionalArg;
        }

        public String key() {
            return longArg != null ? longArg.substring(2) : shortArg.substring(1);
        }

        public boolean match(String option) {
            return option.equals(shortArg) || option.equals(longArg) || option.equals(additionalArg);
        }

        public String getShortArg() {
            return shortArg;
        }

        public String getLongArg() {
            return longArg;
        }

        /**
         * An example of how the argument is used.
         *
         * @return the example.
         */
        public String argumentExample() {
            return (null != getShortArg() ? getShortArg() : "").concat(null != getLongArg() ? ", " + getLongArg() : "");
        }

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

        public static void printUsage(ConsoleWrapper consoleWrapper) {
            consoleWrapper.printf(usage());
        }

        public static String usage() {
            if (USAGE == null) {
                final StringBuilder sb = new StringBuilder();
                sb.append(MESSAGES.argUsage()).append(NEW_LINE);
                for (CommandLineArgument arg : CommandLineArgument.values()) {
                    if (arg != ROLE) { // Deprecated
                        sb.append(arg.toString()).append(NEW_LINE);
                    }
                }
                USAGE = sb.toString();
            }
            return USAGE;
        }
    }
}
