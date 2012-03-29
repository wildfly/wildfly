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


package org.jboss.as.domain.management.security.state;

import org.jboss.as.domain.management.security.ConsoleWrapper;

import static org.jboss.as.domain.management.security.AddPropertiesUser.DEFAULT_MANAGEMENT_REALM;
import static org.jboss.as.domain.management.security.AddPropertiesUser.NEW_LINE;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * State to prompt the user for the realm, username and password to use, this State can be called back to so allows for a
 * pre-defined realm and username to be used.
 */
public class PromptNewUserState implements State {
    private final StateValues stateValues;
    private ConsoleWrapper theConsole;

    public PromptNewUserState(ConsoleWrapper theConsole) {
        this.theConsole = theConsole;
        stateValues = new StateValues();
        stateValues.setRealm(DEFAULT_MANAGEMENT_REALM);
    }

    public PromptNewUserState(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
    }

    @Override
    public State execute() {
        State continuingState = new PromptPasswordState(theConsole, stateValues);
        if (stateValues.isSilentOrNonInteractive() == false) {
            theConsole.printf(NEW_LINE);
            theConsole.printf(MESSAGES.enterNewUserDetails());
            theConsole.printf(NEW_LINE);
            stateValues.setPassword(null); // If interactive we want to be sure to capture this.

            /*
            * Prompt for realm.
            */
            theConsole.printf(MESSAGES.realmPrompt(stateValues.getRealm()));
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
                stateValues.setRealm(temp);
            }

            /*
            * Prompt for username.
            */
            String existingUsername = stateValues.getUserName();
            String usernamePrompt = existingUsername == null ? MESSAGES.usernamePrompt() :
                    MESSAGES.usernamePrompt(existingUsername);
            theConsole.printf(usernamePrompt);
            temp = theConsole.readLine(" : ");
            if (temp != null && temp.length() > 0) {
                existingUsername = temp;
            }
            // The user could have pressed Ctrl-D, in which case we do not use the default value.
            if (temp == null || existingUsername == null || existingUsername.length() == 0) {
                return new ErrorState(theConsole,MESSAGES.noUsernameExiting());
            }
            stateValues.setUserName(existingUsername);

            if (stateValues.getKnownUsers().contains(stateValues.getUserName())) {
                State duplicateContinuing = stateValues.isSilentOrNonInteractive() ? null : new PromptNewUserState(theConsole, stateValues);
                if (stateValues.isSilentOrNonInteractive()) {
                    continuingState = new ErrorState(theConsole, MESSAGES.duplicateUser(stateValues.getUserName()), duplicateContinuing, stateValues);
                } else {
                    String message = MESSAGES.aboutToUpdateUser(stateValues.getUserName());
                    String prompt = MESSAGES.isCorrectPrompt();

                    stateValues.setExistingUser(true);
                    continuingState = new ConfirmationChoice(theConsole,message, prompt, continuingState, duplicateContinuing);
                }
            }
        }

        return continuingState;
    }

}