/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.jboss.as.domain.management.security.ConsoleWrapper;

/**
 * State to perform validation of the supplied username.
 *
 * Checks include: - Valid characters. Easy to guess user names. Duplicate users.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ValidateUserState extends AbstractValidationState {

    private static final char[] VALID_PUNCTUATION;

    static {
        char[] validPunctuation = { '.', '@', '\\', '=', ',', '/' };
        Arrays.sort(validPunctuation);
        VALID_PUNCTUATION = validPunctuation;
    }

    private final StateValues stateValues;
    private final ConsoleWrapper theConsole;

    public ValidateUserState(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
    }

    @Override
    protected Collection<State> getValidationStates() {
        List<State> validationStates = new ArrayList<State>(3);
        validationStates.add(getValidCharactersState());
        validationStates.add(getDuplicateCheckState());
        validationStates.add(getCommonNamesCheckState());

        return validationStates;
    }

    @Override
    protected State getSuccessState() {
        return new PromptPasswordState(theConsole, stateValues, false);
    }

    private State getRetryState() {
        return stateValues.isSilentOrNonInteractive() ? null : new PromptNewUserState(theConsole, stateValues);
    }

    private State getValidCharactersState() {
        return new State() {

            @Override
            public State execute() {
                for (char currentChar : stateValues.getUserName().toCharArray()) {
                    if ((!isValidPunctuation(currentChar))
                            && (Character.isLetter(currentChar) || Character.isDigit(currentChar)) == false) {
                        return new ErrorState(theConsole, MESSAGES.usernameNotAlphaNumeric(), getRetryState(), stateValues);
                    }
                }

                return ValidateUserState.this;
            }

            private boolean isValidPunctuation(char currentChar) {
                return (Arrays.binarySearch(VALID_PUNCTUATION, currentChar) >= 0);
            }
        };
    }

    private State getDuplicateCheckState() {
        return new State() {

            @Override
            public State execute() {
                if (stateValues.getKnownUsers().contains(stateValues.getUserName())) {
                    State duplicateContinuing = stateValues.isSilentOrNonInteractive() ? null : new PromptNewUserState(
                            theConsole, stateValues);
                    if (stateValues.isSilentOrNonInteractive()) {
                        return new ErrorState(theConsole, MESSAGES.duplicateUser(stateValues.getUserName()),
                                duplicateContinuing, stateValues);
                    } else {
                        String message = MESSAGES.aboutToUpdateUser(stateValues.getUserName());
                        String prompt = MESSAGES.isCorrectPrompt() + " " + MESSAGES.yes() + "/" + MESSAGES.no() + "?";

                        stateValues.setExistingUser(true);
                        return new ConfirmationChoice(theConsole, message, prompt, ValidateUserState.this, duplicateContinuing);
                    }
                } else {
                    stateValues.setExistingUser(false);

                    return ValidateUserState.this;
                }
            }
        };
    }

    private State getCommonNamesCheckState() {
        return new State() {

            @Override
            public State execute() {
                // If this is updating an existing user then the name is already accepted.
                if (stateValues.isExistingUser() == false && stateValues.isSilentOrNonInteractive() == false) {
                    for (String current : BAD_USER_NAMES) {
                        if (current.equals(stateValues.getUserName().toLowerCase(Locale.ENGLISH))) {
                            String message = MESSAGES.usernameEasyToGuess(stateValues.getUserName());
                            String prompt = MESSAGES.sureToAddUser(stateValues.getUserName());

                            return new ConfirmationChoice(theConsole, message, prompt, ValidateUserState.this, getRetryState());
                        }
                    }
                }

                return ValidateUserState.this;
            }
        };
    }

}
