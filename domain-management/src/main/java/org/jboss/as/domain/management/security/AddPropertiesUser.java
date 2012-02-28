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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jboss.sasl.util.UsernamePasswordHashUtil;

/**
 * A command line utility to add new users to the mgmt-users.properties files.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AddPropertiesUser {

    private static final String[] BAD_USER_NAMES = {"admin", "administrator", "root"};

    private static final String DEFAULT_MANAGEMENT_REALM = "ManagementRealm";
    private static final String DEFAULT_APPLICATION_REALM = "ApplicationRealm";
    public static final String MGMT_USERS_PROPERTIES = "mgmt-users.properties";
    public static final String APPLICATION_USERS_PROPERTIES = "application-users.properties";
    public static final String APPLICATION_ROLES_PROPERTIES = "application-roles.properties";
    public static final String APPLICATION_USERS_SWITCH = "-a";

    private static final char NEW_LINE_CHAR = '\n';
    private static final char CARRIAGE_RETURN_CHAR = '\r';

    private static final String NEW_LINE = "\n";
    private static final String SPACE = " ";
    private static final Properties argsCliProps = new Properties();

    private final Console theConsole = System.console();

    private List<File> propertiesFiles;
    private List<File> roleFiles;
    private Set<String> knownUsers;
    private State nextState;

    private AddPropertiesUser() {
        if (theConsole == null) {
            throw MESSAGES.noConsoleAvailable();
        }
        nextState = new PropertyFilePrompt();
    }

    private AddPropertiesUser(final boolean management, final String user, final char[] password, final String realm) {
        boolean silent = false;
        Values values = new Values();

        String valueSilent = argsCliProps.getProperty("silent");

        if (valueSilent != null) {
            silent = Boolean.valueOf(valueSilent);
        }
        if (silent) {
            values.howInteractive = Interactiveness.SILENT;
        } else {
            values.howInteractive = Interactiveness.NON_INTERACTIVE;
        }

        if ((theConsole == null) && (values.isSilent() == false)) {
            throw MESSAGES.noConsoleAvailable();
        }
        values.userName = user;
        values.password = password;
        values.realm = realm;
        values.management = management;

        nextState = new PropertyFileFinder(values);
    }

    private AddPropertiesUser(boolean management, final String user, final char[] password) {
        this(management, user, password, management ? DEFAULT_MANAGEMENT_REALM : DEFAULT_APPLICATION_REALM);
    }

    private void run() {
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


    private class PropertyFilePrompt implements State {

        private static final int MANAGEMENT = 0;
        private static final int APPLICATION = 1;
        private static final int INVALID = 2;

        @Override
        public State execute() {

            Values values = new Values();
            theConsole.printf(NEW_LINE);
            theConsole.printf(MESSAGES.filePrompt());
            theConsole.printf(NEW_LINE);

            while (true) {
                String temp = theConsole.readLine("(a): ");
                if (temp == null) {
                    /*
                     * This will return user to the command prompt so add a new line to ensure the command prompt is on the next
                     * line.
                     */
                    theConsole.printf(NEW_LINE);
                    return null;
                }

                if (temp.length() > 0) {
                    switch (convertResponse(temp)) {
                        case MANAGEMENT:
                            values.management = true;
                            values.realm = DEFAULT_MANAGEMENT_REALM;
                            return new PropertyFileFinder(values);
                        case APPLICATION:
                            values.management = false;
                            values.realm = DEFAULT_APPLICATION_REALM;
                            return new PropertyFileFinder(values);
                        default:
                            return new ErrorState(MESSAGES.invalidChoiceResponse(), this);
                    }
                } else {
                    values.management = true;
                    values.realm = DEFAULT_MANAGEMENT_REALM;
                    return new PropertyFileFinder(values);
                }
            }
        }

        private int convertResponse(final String response) {
            String temp = response.toLowerCase();
            if ("A".equals(temp) || "a".equals(temp)) {
                return MANAGEMENT;
            }

            if ("B".equals(temp) || "b".equals(temp)) {
                return APPLICATION;
            }

            return INVALID;
        }

    }

    /**
     * The first state executed, responsible for searching for the relevant properties files.
     */
    private class PropertyFileFinder implements State {

        private final Values values;

        private PropertyFileFinder(final Values values) {
            this.values = values;
        }

        @Override
        public State execute() {
            String jbossHome = System.getenv("JBOSS_HOME");
            if (jbossHome == null) {
                return new ErrorState(MESSAGES.jbossHomeNotSet(), null, values);
            }

            List<File> foundFiles = new ArrayList<File>(2);
            final String fileName = values.management ? MGMT_USERS_PROPERTIES : APPLICATION_USERS_PROPERTIES;
            if (!findFiles(jbossHome, foundFiles, fileName)) {
                return new ErrorState(MESSAGES.propertiesFileNotFound(fileName), null, values);
            }
            if(!values.management) {
                List<File> foundRoleFiles = new ArrayList<File>(2);
                if (!findFiles(jbossHome, foundRoleFiles, APPLICATION_ROLES_PROPERTIES)) {
                    return new ErrorState(MESSAGES.propertiesFileNotFound(APPLICATION_ROLES_PROPERTIES), null, values);
                }
                roleFiles = foundRoleFiles;
            }

            propertiesFiles = foundFiles;

            Set<String> foundUsers = new HashSet<String>();
            for (File current : propertiesFiles) {
                try {
                    foundUsers.addAll(loadUserNames(current));
                } catch (IOException e) {
                    return new ErrorState(MESSAGES.unableToLoadUsers(current.getAbsolutePath(), e.getMessage()), null, values);
                }
            }
            knownUsers = foundUsers;

            if (values == null) {
                return new PromptNewUserState();
            } else {
                return new PromptNewUserState(values);
            }
        }

        private boolean findFiles(final String jbossHome, final List<File> foundFiles, final String fileName) {
            File standaloneProps = new File(jbossHome + "/standalone/configuration/" + fileName);
            if (standaloneProps.exists()) {
                foundFiles.add(standaloneProps);
            }
            File domainProps = new File(jbossHome + "/domain/configuration/" + fileName);
            if (domainProps.exists()) {
                foundFiles.add(domainProps);
            }

            if (foundFiles.size() == 0) {
                return false;
            }
            return true;
        }

        private Set<String> loadUserNames(final File file) throws IOException {

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                Properties tempProps = new Properties();
                tempProps.load(fis);

                return tempProps.stringPropertyNames();
            } finally {
                safeClose(fis);
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
            values.realm = DEFAULT_MANAGEMENT_REALM;
        }

        PromptNewUserState(final Values values) {
            this.values = values;
        }

        @Override
        public State execute() {
            if (values.isSilentOrNonInteractive() == false) {
                theConsole.printf(NEW_LINE);
                theConsole.printf(MESSAGES.enterNewUserDetails());
                theConsole.printf(NEW_LINE);
                values.password = null; // If interactive we want to be sure to capture this.

                /*
                 * Prompt for realm.
                 */
                theConsole.printf(MESSAGES.realmPrompt(values.realm));
                String temp = theConsole.readLine(" : ");
                if (temp == null) {
                    /*
                     * This will return user to the command prompt so add a new line to
                     * ensure the command prompt is on the next line.
                     */
                    theConsole.printf(NEW_LINE);
                    return null;
                }
                if (temp.length() > 0) {
                    values.realm = temp;
                }

                /*
                 * Prompt for username.
                 */
                String existingUsername = values.userName;
                String usernamePrompt = existingUsername == null ? MESSAGES.usernamePrompt() :
                        MESSAGES.usernamePrompt(existingUsername);
                theConsole.printf(usernamePrompt);
                temp = theConsole.readLine(" : ");
                if (temp != null && temp.length() > 0) {
                    existingUsername = temp;
                }
                // The user could have pressed Ctrl-D, in which case we do not use the default value.
                if (temp == null || existingUsername == null || existingUsername.length() == 0) {
                    return new ErrorState(MESSAGES.noUsernameExiting());
                }
                values.userName = existingUsername;

                /*
                 * Prompt for password.
                 */
                theConsole.printf(MESSAGES.passwordPrompt());
                char[] tempChar = theConsole.readPassword(" : ");
                if (tempChar == null || tempChar.length == 0) {
                    return new ErrorState(MESSAGES.noPasswordExiting());
                }

                theConsole.printf(MESSAGES.passwordConfirmationPrompt());
                char[] secondTempChar = theConsole.readPassword(" : ");
                if (secondTempChar == null) {
                    secondTempChar = new char[0]; // If re-entry missed allow fall through to comparison.
                }

                if (Arrays.equals(tempChar, secondTempChar) == false) {
                    return new ErrorState(MESSAGES.passwordMisMatch(), this);
                }
                values.password = tempChar;

                if(!values.management) {
                    theConsole.printf(MESSAGES.rolesPrompt());
                    values.roles = theConsole.readLine(" : ");
                }
            }

            return new WeakCheckState(values);
        }

    }

    /**
     * State to check the strength of the values selected.
     * <p/>
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
            State retryState = values.isSilentOrNonInteractive() ? null : new PromptNewUserState(values);

            if (Arrays.equals(values.userName.toCharArray(), values.password)) {
                return new ErrorState(MESSAGES.usernamePasswordMatch(), retryState);
            }

            for (char currentChar : values.userName.toCharArray()) {
                if ((Character.isLetter(currentChar) || Character.isDigit(currentChar)) == false) {
                    return new ErrorState(MESSAGES.usernameNotAlphaNumeric(), retryState);
                }
            }

            boolean weakUserName = false;
            for (String current : BAD_USER_NAMES) {
                if (current.equals(values.userName.toLowerCase())) {
                    weakUserName = true;
                    break;
                }
            }

            State continuingState = new DuplicateUserCheckState(values);
            if (weakUserName && values.isSilentOrNonInteractive() == false) {
                String message = MESSAGES.usernameEasyToGuess(values.userName);
                String prompt = MESSAGES.sureToAddUser(values.userName);
                State noState = new PromptNewUserState(values);

                return new ConfirmationChoice(message, prompt, continuingState, noState);
            }

            return continuingState;
        }

    }

    /**
     * State to check that the user is not already defined in any of the resolved
     * properties files.
     */
    private class DuplicateUserCheckState implements State {

        private Values values;

        private DuplicateUserCheckState(final Values values) {
            this.values = values;
        }

        @Override
        public State execute() {
            if (knownUsers.contains(values.userName)) {
                State continuing = values.isSilentOrNonInteractive() ? null : new PromptNewUserState(values);

                return new ErrorState(MESSAGES.duplicateUser(values.userName), continuing, values);
            }

            State addState = new AddUser(values);
            final State continuingState;
            if (values.isSilentOrNonInteractive()) {
                continuingState = addState;
            } else {
                String message = MESSAGES.aboutToAddUser(values.userName, values.realm);
                String prompt = MESSAGES.isCorrectPrompt();

                continuingState = new ConfirmationChoice(message, prompt, addState, new PromptNewUserState(values));
            }

            return continuingState;
        }


    }

    /**
     * State to display a message to the user with option to confirm a choice.
     * <p/>
     * This state handles either a yes or no outcome and will loop with an error
     * on invalid input.
     */
    private class ConfirmationChoice implements State {

        private final String message;
        private final String prompt;
        private final State yesState;
        private final State noState;

        private static final int YES = 0;
        private static final int NO = 1;
        private static final int INVALID = 2;

        private ConfirmationChoice(final String message, final String prompt, final State yesState, final State noState) {
            this.message = message;
            this.prompt = prompt;
            this.yesState = yesState;
            this.noState = noState;
        }

        @Override
        public State execute() {
            if (message != null) {
                theConsole.printf(message);
                theConsole.printf(NEW_LINE);
            }

            theConsole.printf(prompt);
            String temp = theConsole.readLine(SPACE);

            switch (convertResponse(temp)) {
                case YES:
                    return yesState;
                case NO:
                    return noState;
                default:
                    return new ErrorState(MESSAGES.invalidConfirmationResponse(), this);
            }
        }

        private int convertResponse(final String response) {
            if (response != null) {
                String temp = response.toLowerCase();
                if ("yes".equals(temp) || "y".equals(temp)) {
                    return YES;
                }

                if ("no".equals(temp) || "n".equals(temp)) {
                    return NO;
                }
            }

            return INVALID;
        }

    }

    /**
     * State to perform the actual addition to the discovered properties files.
     * <p/>
     * By this time ALL validation should be complete, this State will only fail for IOExceptions encountered
     * performing the actual writes.
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
                return new ErrorState(e.getMessage(), null, values);
            }

            for (File current : propertiesFiles) {
                try {
                    append(entry, current);
                    if (values.isSilent() == false) {
                        theConsole.printf(MESSAGES.addedUser(values.userName, current.getCanonicalPath()));
                        theConsole.printf(NEW_LINE);
                    }
                } catch (IOException e) {
                    return new ErrorState(MESSAGES.unableToAddUser(current.getAbsolutePath(), e.getMessage()), null, values);
                }
            }

            if(!values.management && values.roles != null && values.roles.length() > 0) {
                for (final File current : roleFiles) {
                    String role = values.userName + "=" + values.roles;
                    try {
                        append(role, current);
                        if (values.isSilent() == false) {
                            theConsole.printf(MESSAGES.addedRoles(values.userName, values.roles, current.getCanonicalPath()));
                            theConsole.printf(NEW_LINE);
                        }
                    } catch (IOException e) {
                        return new ErrorState(MESSAGES.unableToAddUser(current.getAbsolutePath(), e.getMessage()), null, values);
                    }
                }
            }

            /*
             * At this point the files have been written and confirmation passed back so nothing else to do.
             */
            return null;
        }

        private boolean additionalNewLineNeeded(final File file) throws IOException {
            FileReader fr = null;

            try {
                fr = new FileReader(file);
                char lastChar = 0x00;
                char[] temp = new char[1024];

                int read = -1;
                while ((read = fr.read(temp)) > 0) {
                    lastChar = temp[read - 1];
                }
                /*
                 * It is possible that the final line will also have some whitespace - in that case we want
                 * a new line otherwise the line we add could become indented.
                 *
                 * Depending on where the file was last written the character sequence for a new line can vary,
                 * if we see either of the characters used for a new line as the last character of the last line
                 * we assume a new line is already present in the file.
                 */
                return lastChar != NEW_LINE_CHAR && lastChar != CARRIAGE_RETURN_CHAR;
            } finally {
                safeClose(fr);
            }
        }

        private void append(final String entry, final File toFile) throws IOException {
            FileWriter fw = null;
            BufferedWriter bw = null;

            boolean additionalNewLineNeeded = additionalNewLineNeeded(toFile);

            try {
                fw = new FileWriter(toFile, true);
                bw = new BufferedWriter(fw);

                if (additionalNewLineNeeded) {
                    bw.newLine();
                }

                bw.append(entry);
                bw.newLine();
            } finally {
                safeClose(bw);
                safeClose(fw);
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
        private final Values values;

        private ErrorState(String errorMessage) {
            this(errorMessage, null, null);
        }

        private ErrorState(String errorMessage, State nextState) {
            this(errorMessage, nextState, null);
        }

        private ErrorState(String errorMessage, State nextState, Values values) {
            this.errorMessage = errorMessage;
            this.nextState = nextState;
            this.values = values;
        }

        @Override
        public State execute() {
            if ((values == null) || (values != null) && (values.isSilent() == false)) {
                theConsole.printf(NEW_LINE);
                theConsole.printf(" * ");
                theConsole.printf(MESSAGES.errorHeader());
                theConsole.printf(" * ");
                theConsole.printf(NEW_LINE);

                theConsole.printf(errorMessage);
                theConsole.printf(NEW_LINE);
                theConsole.printf(NEW_LINE);
            }
            return nextState;
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

    private class Values {
        private Interactiveness howInteractive = Interactiveness.INTERACTIVE;
        private String realm;
        private String userName;
        private char[] password;
        private boolean management;
        private String roles;

        private boolean isSilentOrNonInteractive() {
            return (howInteractive == Interactiveness.NON_INTERACTIVE) || isSilent();
        }

        private boolean isSilent() {
            return (howInteractive == Interactiveness.SILENT);
        }
    }

    private enum Interactiveness {
        SILENT, NON_INTERACTIVE, INTERACTIVE
    }

    private interface State {

        State execute();

    }

}
