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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.sasl.util.UsernamePasswordHashUtil;

/**
 * A command line utility to add new users to the mgmt-users.properties files.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AddPropertiesUser {

    private static final String[] badUsernames = { "admin", "administrator", "root" };

    private static final String DEFAULT_REALM = "ManagementRealm";

    private Console theConsole = System.console();

    private List<File> propertiesFiles;
    private State nextState;

    private AddPropertiesUser() {
        if (theConsole == null) {
            throw new IllegalStateException("No console available.");
        }
        nextState = new PropertyFileFinder();
    }

    private AddPropertiesUser(final String user, final char[] password, final String realm) {
        if (theConsole == null) {
            throw new IllegalStateException("No console available.");
        }
        Values values = new Values();
        values.nonInteractive = true;
        values.userName = user;
        values.password = password;
        values.realm = realm;

        nextState = new PropertyFileFinder(values);
    }

    private AddPropertiesUser(final String user, final char[] password) {
        this(user, password, DEFAULT_REALM);
    }

    private void run() {
        while ((nextState = nextState.execute()) != null) {
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 3) {
            new AddPropertiesUser(args[0], args[1].toCharArray(), args[2]).run();
        } else if (args.length == 2) {
            new AddPropertiesUser(args[0], args[1].toCharArray()).run();
        } else {
            new AddPropertiesUser().run();
        }
    }

    /**
     * The first state executed, responsible for searching for the relevent properties files.
     */
    private class PropertyFileFinder implements State {

        private final Values values;

        private PropertyFileFinder() {
            values = null;
        }

        private PropertyFileFinder(final Values values) {
            this.values = values;
        }

        @Override
        public State execute() {
            String jbossHome = System.getenv("JBOSS_HOME");
            if (jbossHome == null) {
                return new ErrorState("JBOSS_HOME environment variable not set.");
            }

            List<File> foundFiles = new ArrayList<File>(2);
            File standaloneProps = new File(jbossHome + "/standalone/configuration/mgmt-users.properties");
            if (standaloneProps.exists()) {
                foundFiles.add(standaloneProps);
            }
            File domainProps = new File(jbossHome + "/domain/configuration/mgmt-users.properties");
            if (domainProps.exists()) {
                foundFiles.add(domainProps);
            }

            if (foundFiles.size() == 0) {
                return new ErrorState("No mgmt-users.properties files found.");
            }

            propertiesFiles = foundFiles;

            if (values == null) {
                return new PromptNewUserState();
            } else {
                return new PromptNewUserState(values);
            }
        }

    }

    /**
     * State to prompt the user for the realm, username and password to use, this State can be called back to so allows for a
     * pre-defined realm and username to be used.
     */
    private class PromptNewUserState implements State {
        private final Values values;

        PromptNewUserState() {
            values = new Values();
            values.realm = DEFAULT_REALM;
        }

        PromptNewUserState(final Values values) {
            this.values = values;
        }

        @Override
        public State execute() {
            if (values.nonInteractive == false) {
                theConsole.printf("\nEnter details of new user to add.\n");

                /*
                 * Prompt for realm.
                 */
                String temp = theConsole.readLine("Realm (%s) : ", values.realm);
                if (temp == null) {
                    theConsole.printf("\n");
                    return null;
                }
                if (temp.length() > 0) {
                    values.realm = temp;
                }

                /*
                 * Prompt for username.
                 */
                if (values.userName == null) {
                    temp = theConsole.readLine("Username : ");
                    if (temp == null || temp.length() == 0) {
                        theConsole.printf("\n");
                        return null;
                    }
                    values.userName = temp;
                } else {
                    temp = theConsole.readLine("Username (%s): ", values.userName);
                    if (temp == null) {
                        theConsole.printf("\n");
                        return null;
                    }
                    if (temp.length() > 0) {
                        values.userName = temp;
                    }
                }

                /*
                 * Prompt for password.
                 */
                char[] tempChar = theConsole.readPassword("Password : ");
                if (tempChar == null || tempChar.length == 0) {
                    theConsole.printf("\n");
                    return null;
                }

                char[] secondTempChar = theConsole.readPassword("Re-enter Password : ");
                if (tempChar == null || tempChar.length == 0) {
                    theConsole.printf("\n");
                    return null;
                }

                if (Arrays.equals(tempChar, secondTempChar) == false) {
                    return new ErrorState("Passwords to not match", this);
                }
                values.password = tempChar;
            }

            return new WeakCheckState(values);
        }

    }

    /**
     * State to check the strength of the values selected.
     *
     * TODO - Currently only very basic checks are performed, this could be updated to perform additional password strength
     * checks.
     */
    private class WeakCheckState implements State {

        private Values values;

        private WeakCheckState(Values values) {
            this.values = values;
        }

        @Override
        public State execute() {
            if (Arrays.equals(values.userName.toCharArray(), values.password)) {
                if (values.nonInteractive) {
                    return new ErrorState("Username must not match the password");
                } else {
                    return new ErrorState("Username must not match the password", new PromptNewUserState(values));
                }
            }

            for (char currentChar : values.userName.toCharArray()) {
                if ((Character.isLetter(currentChar) || Character.isDigit(currentChar)) == false) {
                    if (values.nonInteractive) {
                        return new ErrorState("Only alpha/numeric usernames accepted.");
                    } else {
                        values.userName = null;
                        return new ErrorState("Only alpha/numeric usernames accepted.", new PromptNewUserState(values));
                    }
                }
            }

            boolean weakUserName = false;
            for (String current : badUsernames) {
                if (current.equals(values.userName.toLowerCase())) {
                    weakUserName = true;
                    break;
                }
            }

            if (weakUserName && values.nonInteractive == false) {
                theConsole.printf("The username '%s' is easy to guess\n", values.userName);
                String temp = theConsole.readLine("Are you sure you want to add user '%s' yes/no? ", values.userName);
                boolean yes = temp != null && "yes".equals(temp.toLowerCase());

                if (yes == false) {
                    values.userName = null;
                    return new PromptNewUserState(values);
                }
            }

            if (values.nonInteractive) {
                return new AddUser(values);
            } else {
                return new ConfirmNewUser(values);
            }

        }

    }

    /**
     * State to give user a final opportunity to accept the user they are adding.
     */
    private class ConfirmNewUser implements State {

        private final Values values;

        private ConfirmNewUser(final Values values) {
            this.values = values;
        }

        @Override
        public State execute() {
            theConsole.printf("About to add user '%s' for realm '%s'\n", values.userName, values.realm);
            String temp = theConsole.readLine("Is this correct yes/no? ");

            boolean yes = temp != null && "yes".equals(temp.toLowerCase());

            if (yes == false) {
                return new PromptNewUserState();
            }

            return new AddUser(values);
        }

    }

    /**
     * State to perform the actual addition to the discovered properties files.
     */
    private class AddUser implements State {

        private final Values values;

        private AddUser(final Values values) {
            this.values = values;
        }

        @Override
        public State execute() {
            String entry;

            try {
                String hash = new UsernamePasswordHashUtil().generateHashedHexURP(values.userName, values.realm,
                        values.password);
                entry = values.userName + "=" + hash;
            } catch (NoSuchAlgorithmException e) {
                return new ErrorState(e.getMessage());
            }

            for (File current : propertiesFiles) {
                try {
                    append(entry, current);
                    theConsole.printf("Added user '%s' to file '%s'\n", values.userName, current.getCanonicalPath());
                } catch (IOException e) {
                    return new ErrorState("Unable to add user to " + current.getAbsolutePath() + " due to error "
                            + e.getMessage());
                }
            }

            /*
             * At this point the files have been written and confirmation passed back so nothing else to do.
             */
            return null;
        }

        private void append(final String entry, final File toFile) throws IOException {
            FileWriter fw = null;
            BufferedWriter bw = null;

            try {
                fw = new FileWriter(toFile, true);
                bw = new BufferedWriter(fw);

                bw.append(entry);
                bw.newLine();
            } finally {
                safeClose(bw);
                safeClose(fw);
            }

        }

        private void safeClose(Closeable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                }
            }
        }

    }

    /**
     * State to report an error to the user, optionally a nextState can be supplied so the process can continue even though an
     * error has been reported.
     */
    private class ErrorState implements State {

        private final State nextState;
        private final String errorMessage;

        private ErrorState(String errorMessage) {
            this(errorMessage, null);
        }

        private ErrorState(String errorMessage, State nextState) {
            this.errorMessage = errorMessage;
            this.nextState = nextState;
        }

        @Override
        public State execute() {
            theConsole.printf("\n* Error * \n");
            theConsole.printf("Unable to add user due to error '%s'\n\n", errorMessage);

            return nextState;
        }

    }

    private class Values {
        private boolean nonInteractive = false;
        private String realm;
        private String userName;
        private char[] password;
    }

    private interface State {

        State execute();

    }

}
