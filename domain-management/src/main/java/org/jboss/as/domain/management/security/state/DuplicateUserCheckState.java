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

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * State to check that the user is not already defined in any of the resolved
 * properties files.
 */
public class DuplicateUserCheckState implements State {

    private final ConsoleWrapper theConsole;
    private StateValues stateValues;

    public DuplicateUserCheckState(final ConsoleWrapper theConsole,final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    @Override
    public State execute() {
        final State continuingState;
        if (stateValues.isExistingUser()) {
            continuingState = new UpdateUser(theConsole, stateValues);
        } else {
            State addState = new AddUser(theConsole, stateValues);

            if (stateValues.isSilentOrNonInteractive()) {
                continuingState = addState;
            } else {
                String message = MESSAGES.aboutToAddUser(stateValues.getUserName(), stateValues.getRealm());
                String prompt = MESSAGES.isCorrectPrompt();

                continuingState = new ConfirmationChoice(theConsole,message, prompt, addState, new PromptNewUserState(theConsole, stateValues));
            }
        }

        return continuingState;
    }


}

