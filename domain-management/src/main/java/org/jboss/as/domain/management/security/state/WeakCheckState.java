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
import static org.jboss.as.domain.management.security.AddPropertiesUser.BAD_USER_NAMES;

import java.util.Arrays;
import java.util.Locale;

import org.jboss.as.domain.management.security.ConsoleWrapper;

/**
 * State to check the strength of the stateValues selected.
 * <p/>
 * TODO - Currently only very basic checks are performed, this could be updated to perform additional password strength
 * checks.
 */
public class WeakCheckState implements State {

    private ConsoleWrapper theConsole;
    private StateValues stateValues;
    private static char[] VALID_PUNCTUATION = {'.', '@', '\\', '=', ',','/'};

    public WeakCheckState(ConsoleWrapper theConsole,StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    private boolean isValidPunctuation(char currentChar) {
        Arrays.sort(VALID_PUNCTUATION);
        return (Arrays.binarySearch(VALID_PUNCTUATION,currentChar) >= 0);
    }

    @Override
    public State execute() {
        State retryState = stateValues.isSilentOrNonInteractive() ? null : new PromptNewUserState(theConsole, stateValues);

        if (Arrays.equals(stateValues.getUserName().toCharArray(), stateValues.getPassword())) {
            return new ErrorState(theConsole, MESSAGES.usernamePasswordMatch(), retryState);
        }

        for (char currentChar : stateValues.getUserName().toCharArray()) {
            if ((!isValidPunctuation(currentChar)) && (Character.isLetter(currentChar) || Character.isDigit(currentChar)) == false) {
                return new ErrorState(theConsole, MESSAGES.usernameNotAlphaNumeric(), retryState);
            }
        }

        boolean weakUserName = false;
        for (String current : BAD_USER_NAMES) {
            if (current.equals(stateValues.getUserName().toLowerCase(Locale.ENGLISH))) {
                weakUserName = true;
                break;
            }
        }

        State continuingState = new DuplicateUserCheckState(theConsole, stateValues);
        if (weakUserName && stateValues.isSilentOrNonInteractive() == false) {
            String message = MESSAGES.usernameEasyToGuess(stateValues.getUserName());
            String prompt = MESSAGES.sureToAddUser(stateValues.getUserName());
            State noState = new PromptNewUserState(theConsole, stateValues);

            return new ConfirmationChoice(theConsole,message, prompt, continuingState, noState);
        }

        return continuingState;
    }

}