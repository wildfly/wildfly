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

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import org.jboss.as.domain.management.security.ConsoleWrapper;

/**
 * State to prompt the user for the realm, username and password to use, this State can be called back to so allows for a
 * pre-defined realm and username to be used.
 */
public class PromptNewUserState implements State {
    private final StateValues stateValues;
    private ConsoleWrapper theConsole;

    public PromptNewUserState(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
        if ((stateValues != null && stateValues.isSilent() == false) && theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    @Override
    public State execute() {
        State continuingState = new ValidateUserState(theConsole, stateValues);
        if (stateValues.isSilentOrNonInteractive() == false) {
            stateValues.setPassword(null); // If interactive we want to be sure to capture this.

            /*
            * Prompt for username.
            */
            String existingUsername = stateValues.getUserName();
            String usernamePrompt = existingUsername == null ? MESSAGES.usernamePrompt() :
                    MESSAGES.usernamePrompt(existingUsername);
            theConsole.printf(usernamePrompt);
            String temp = theConsole.readLine(" : ");
            if (temp != null && temp.length() > 0) {
                existingUsername = temp;
            }
            // The user could have pressed Ctrl-D, in which case we do not use the default value.
            if (temp == null || existingUsername == null || existingUsername.length() == 0) {
                return new ErrorState(theConsole,MESSAGES.noUsernameExiting());
            }
            stateValues.setUserName(existingUsername);
        }

        return continuingState;
    }

}