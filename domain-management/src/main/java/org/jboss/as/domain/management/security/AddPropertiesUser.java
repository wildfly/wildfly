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

import org.jboss.as.domain.management.security.state.PropertyFileFinder;
import org.jboss.as.domain.management.security.state.PropertyFilePrompt;
import org.jboss.as.domain.management.security.state.State;
import org.jboss.as.domain.management.security.state.StateValues;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * A command line utility to add new users to the mgmt-users.properties files.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AddPropertiesUser {

    public static final String[] BAD_USER_NAMES = {"admin", "administrator", "root"};

    public static final String SERVER_BASE_DIR = "jboss.server.base.dir";
    public static final String SERVER_CONFIG_DIR = "jboss.server.config.dir";
    public static final String DOMAIN_BASE_DIR = "jboss.domain.base.dir";
    public static final String DOMAIN_CONFIG_DIR = "jboss.domain.config.dir";

    public static final String DEFAULT_MANAGEMENT_REALM = "ManagementRealm";
    public static final String DEFAULT_APPLICATION_REALM = "ApplicationRealm";
    public static final String MGMT_USERS_PROPERTIES = "mgmt-users.properties";
    public static final String APPLICATION_USERS_PROPERTIES = "application-users.properties";
    public static final String APPLICATION_ROLES_PROPERTIES = "application-roles.properties";
    public static final String APPLICATION_USERS_SWITCH = "-a";


    private static final char CARRIAGE_RETURN_CHAR = '\r';

    public static final String NEW_LINE = "\n";
    public static final String SPACE = " ";
    private static final Properties argsCliProps = new Properties();


    private ConsoleWrapper theConsole;


    protected State nextState;

    protected AddPropertiesUser() {
        theConsole = new JavaConsole();
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
        nextState = new PropertyFilePrompt(theConsole);
    }

    protected AddPropertiesUser(ConsoleWrapper console) {
        this.theConsole = console;
        nextState = new PropertyFilePrompt(theConsole);
    }

    private AddPropertiesUser(final boolean management, final String user, final char[] password, final String realm) {
        boolean silent = false;
        StateValues stateValues = new StateValues();

        String valueSilent = argsCliProps.getProperty("silent");

        if (valueSilent != null) {
            silent = Boolean.valueOf(valueSilent);
        }
        if (silent) {
            stateValues.setHowInteractive(Interactiveness.SILENT);
        } else {
            stateValues.setHowInteractive(Interactiveness.NON_INTERACTIVE);
        }

        if ((theConsole == null) && (stateValues.isSilent() == false)) {
            throw MESSAGES.noConsoleAvailable();
        }
        stateValues.setUserName(user);
        stateValues.setPassword(password);
        stateValues.setRealm(realm);
        stateValues.setManagement(management);

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

        List<String> argsList = new LinkedList<String>();
        String[] argsArray = null;
        StringReader stringReader = null;
        boolean management = true;

        int realArgsLength;

        if (args.length >= 1) {

            for (String temp : args) {
                if (temp.startsWith("--")) {
                    try {
                        stringReader = new StringReader(temp.substring(2));
                        argsCliProps.load(stringReader);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        safeClose(stringReader);
                    }
                } else if (temp.equals(APPLICATION_USERS_SWITCH)) {
                    management = false;
                } else {
                    argsList.add(temp);
                }
            }
        }
        argsArray = argsList.toArray(new String[0]);
        realArgsLength = argsArray.length;
        if (realArgsLength == 3) {
            new AddPropertiesUser(management, argsArray[0], argsArray[1].toCharArray(), argsArray[2]).run();
        } else if (realArgsLength == 2) {
            new AddPropertiesUser(management, argsArray[0], argsArray[1].toCharArray()).run();
        } else {
            new AddPropertiesUser().run();
        }
    }

    private static void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    public enum Interactiveness {
        SILENT, NON_INTERACTIVE, INTERACTIVE
    }
}
