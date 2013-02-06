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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.domain.management.security.ConsoleWrapper;
import org.jboss.as.domain.management.security.password.PasswordCheckResult;
import org.jboss.as.domain.management.security.password.PasswordCheckUtil;

/**
 * State to perform validation of the supplied password.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ValidatePasswordState extends AbstractValidationState {

    private final StateValues stateValues;
    private final ConsoleWrapper theConsole;

    public ValidatePasswordState(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
    }

    @Override
    protected Collection<State> getValidationStates() {
        List<State> validationStates = new ArrayList<State>(2);
        validationStates.add(getUsernameMatchState());
        validationStates.add(getDetailedCheckState());

        return validationStates;
    }

    private State getRetryState() {
        return stateValues.isSilentOrNonInteractive() ? null : new PromptNewUserState(theConsole, stateValues);
    }

    private State getUsernameMatchState() {
        return new State() {

            @Override
            public State execute() {
                if (Arrays.equals(stateValues.getUserName().toCharArray(), stateValues.getPassword())) {
                    return new ErrorState(theConsole, MESSAGES.usernamePasswordMatch(), getRetryState(), stateValues);
                }

                return ValidatePasswordState.this;
            }

        };
    }

    private State getDetailedCheckState() {
        return new State() {

            @Override
            public State execute() {
                PasswordCheckResult result = PasswordCheckUtil.INSTANCE.check(false, stateValues.getUserName(), new String(
                        stateValues.getPassword()));
                if (result.getResult() == PasswordCheckResult.Result.WARN && stateValues.isSilentOrNonInteractive() == false) {
                    String message = result.getMessage();
                    String prompt = MESSAGES.sureToSetPassword(new String(stateValues.getPassword()));
                    State noState = new PromptNewUserState(theConsole, stateValues);
                    return new ConfirmationChoice(theConsole, message, prompt, ValidatePasswordState.this, noState);
                }

                if (result.getResult() == PasswordCheckResult.Result.REJECT) {
                    return new ErrorState(theConsole, result.getMessage(), getRetryState());
                }

                return ValidatePasswordState.this;
            }

        };
    }

    @Override
    protected State getSuccessState() {
        // We like the password but want the user to re-enter to confirm they know the password.
        return stateValues.isInteractive() ? new PromptPasswordState(theConsole, stateValues, true) : new PreModificationState(
                theConsole, stateValues);
    }

}
