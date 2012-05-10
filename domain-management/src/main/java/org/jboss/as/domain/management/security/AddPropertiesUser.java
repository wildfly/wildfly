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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;


import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;

import org.jboss.as.domain.management.security.state.PropertyFileFinder;
import org.jboss.as.domain.management.security.state.PropertyFilePrompt;
import org.jboss.as.domain.management.security.state.State;
import org.jboss.as.domain.management.security.state.StateValues;

/**
 * A command line utility to add new users to the mgmt-users.properties files.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AddPropertiesUser {

    public static final String[] BAD_USER_NAMES = {"admin", "administrator", "root"};

    public static final String SERVER_BASE_DIR = "jboss.server.base.dir";
    public static final String SERVER_CONFIG_DIR = "jboss.server.config.dir";
    public static final String SERVER_CONFIG_USER_DIR = "jboss.server.config.user.dir";
    public static final String DOMAIN_BASE_DIR = "jboss.domain.base.dir";
    public static final String DOMAIN_CONFIG_DIR = "jboss.domain.config.dir";
    public static final String DOMAIN_CONFIG_USER_DIR = "jboss.domain.config.user.dir";

    public static final String DEFAULT_MANAGEMENT_REALM = "ManagementRealm";
    public static final String DEFAULT_APPLICATION_REALM = "ApplicationRealm";
    public static final String MGMT_USERS_PROPERTIES = "mgmt-users.properties";
    public static final String APPLICATION_USERS_PROPERTIES = "application-users.properties";
    public static final String APPLICATION_ROLES_PROPERTIES = "application-roles.properties";
    public static final String APPLICATION_USERS_OPTION = "-a";
    public static final String DOMAIN_CONFIG_DIR_USERS_OPTION = "-dc";
    public static final String SERVER_CONFIG_DIR_USERS_OPTION = "-sc";

    public static final CommandLineOption PASSWORD_OPTION = new CommandLineOption("-p", "--password");
    public static final CommandLineOption USER_OPTION = new CommandLineOption("-u", "--user");
    public static final CommandLineOption REALM_OPTION = new CommandLineOption("-r", "--realm");
    public static final CommandLineOption SILENT_OPTION = new CommandLineOption("-s", "--silent");
    public static final CommandLineOption ROLE_OPTION = new CommandLineOption("-ro", "--role");

    // List the available command-line options
    public static final CommandLineOption[] COMMAND_LINE_OPTIONS = new CommandLineOption[]{
            PASSWORD_OPTION,
            USER_OPTION,
            REALM_OPTION,
            SILENT_OPTION,
            ROLE_OPTION};

    public static final String NEW_LINE = "\n";
    public static final String SPACE = " ";
    private static final Properties argsCliProps = new Properties();

    private final ConsoleWrapper theConsole;

    protected State nextState;

    protected AddPropertiesUser() {
        theConsole = new JavaConsole();
        StateValues stateValues = new StateValues();
        stateValues.setJbossHome(System.getenv("JBOSS_HOME"));

        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
        nextState = new PropertyFilePrompt(theConsole, stateValues);
    }

    protected AddPropertiesUser(ConsoleWrapper console) {
        this.theConsole = console;
        StateValues stateValues = new StateValues();
        stateValues.setJbossHome(System.getenv("JBOSS_HOME"));
        nextState = new PropertyFilePrompt(theConsole, stateValues);
    }

    private AddPropertiesUser(final boolean management, final String user, final char[] password, final String realm) {
        StateValues stateValues = new StateValues();
        stateValues.setJbossHome(System.getenv("JBOSS_HOME"));

        final Interactiveness howInteractive;
        boolean silent = Boolean.valueOf(argsCliProps.getProperty(SILENT_OPTION.key()));
        if (silent) {
            howInteractive = Interactiveness.SILENT;
        } else {
            howInteractive = Interactiveness.NON_INTERACTIVE;
        }
        stateValues.setHowInteractive(howInteractive);

        // Silent modes still need to be able to output an error on failure.
        theConsole = new JavaConsole();
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
        stateValues.setUserName(user);
        stateValues.setPassword(password);
        stateValues.setRealm(realm);
        stateValues.setManagement(management);
        stateValues.setRoles(argsCliProps.getProperty(ROLE_OPTION.key()));

        nextState = new PropertyFileFinder(theConsole, stateValues);
    }

    private AddPropertiesUser(boolean management, final String user, final char[] password) {
        this(management, user, password, management ? DEFAULT_MANAGEMENT_REALM : DEFAULT_APPLICATION_REALM);
    }

    protected void run() {
        while ((nextState = nextState.execute()) != null) {
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        boolean management = true;

        if (args.length >= 1) {

            Iterator<String> it = Arrays.asList(args).iterator();
            String temp;
            while (it.hasNext()) {
                temp = it.next();
                if (DOMAIN_CONFIG_DIR_USERS_OPTION.equals(temp)) {
                    System.setProperty(DOMAIN_CONFIG_DIR, it.next());
                } else if (SERVER_CONFIG_DIR_USERS_OPTION.equals(temp)) {
                    System.setProperty(SERVER_CONFIG_DIR, it.next());
                } else if (APPLICATION_USERS_OPTION.equals(temp)) {
                    management = false;
                } else {
                    // Find the command-line option
                    CommandLineOption commandLineOption = findCommandLineOption(temp);
                    if (commandLineOption != null) {
                        final String value;
                        if (SILENT_OPTION.equals(commandLineOption)) {
                            value = Boolean.TRUE.toString();
                        } else {
                            value = it.next();
                        }
                        argsCliProps.setProperty(commandLineOption.key(), value);
                    } else {
                        // By default, the first arg without option is the username,
                        if (!argsCliProps.containsKey(USER_OPTION.key())) {
                            argsCliProps.setProperty(USER_OPTION.key(), temp);
                        }
                        // the second arg is the password and,
                        else if (!argsCliProps.containsKey(PASSWORD_OPTION.key())) {
                            argsCliProps.setProperty(PASSWORD_OPTION.key(), temp);
                        }
                        // the third one is the realm.
                        else if (!argsCliProps.containsKey(REALM_OPTION.key())) {
                            argsCliProps.setProperty(REALM_OPTION.key(), temp);
                        }
                    }
                }
            }
        }

        if (argsCliProps.containsKey(PASSWORD_OPTION.key()) && argsCliProps.containsKey(USER_OPTION.key())) {
            char[] password = argsCliProps.getProperty(PASSWORD_OPTION.key()).toCharArray();
            String user = argsCliProps.getProperty(USER_OPTION.key());
            if (argsCliProps.contains(REALM_OPTION.key())) {
                new AddPropertiesUser(management, user, password, argsCliProps.getProperty(REALM_OPTION.key())).run();
            } else {
                new AddPropertiesUser(management, user, password).run();
            }
        } else {
            new AddPropertiesUser().run();
        }
    }

    public enum Interactiveness {
        SILENT, NON_INTERACTIVE, INTERACTIVE
    }

    /**
     * Find the command-line option corresponding to the parameter {@code option}.
     *
     * @param option
     * @return The corresponding option or null.
     */
    private static CommandLineOption findCommandLineOption(String option) {
        for (CommandLineOption commandLineOption : COMMAND_LINE_OPTIONS) {
            if (commandLineOption.match(option)) {
                return commandLineOption;
            }
        }
        return null;
    }

    protected static class CommandLineOption {
        private String shortOption;
        private String longOption;

        private CommandLineOption(String shortOption, String longOption) {
            this.shortOption = shortOption;
            this.longOption = longOption;
        }

        public String key() {
            return longOption.substring(2);
        }

        public boolean match(String option) {
            return shortOption.equals(option) || longOption.equals(option);
        }
    }
}
