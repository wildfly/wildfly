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

import java.util.Arrays;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * State to prompt the user for a password
 * <p/>
 * This state handles password validation by let the user re-enter the password
 * in case of the password mismatch the user will be present for an error
 * and will re-enter the PromptPasswordState again
 */
public class PromptPasswordState implements State {

    private ConsoleWrapper theConsole;
    private StateValues stateValues;

    public PromptPasswordState(ConsoleWrapper theConsole, StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
    }

    @Override
    public State execute() {
        State continuingState = new WeakCheckState(theConsole, stateValues);
        if (stateValues.isSilentOrNonInteractive() == false) {
            /*
            * Prompt for password.
            */
            theConsole.printf(MESSAGES.passwordPrompt());
            char[] tempChar = theConsole.readPassword(" : ");
            if (tempChar == null || tempChar.length == 0) {
                return new ErrorState(theConsole,MESSAGES.noPasswordExiting());
            }

            theConsole.printf(MESSAGES.passwordConfirmationPrompt());
            char[] secondTempChar = theConsole.readPassword(" : ");
            if (secondTempChar == null) {
                secondTempChar = new char[0]; // If re-entry missed allow fall through to comparison.
            }

            if (Arrays.equals(tempChar, secondTempChar) == false) {
                return new ErrorState(null, MESSAGES.passwordMisMatch(), this);
            }
            stateValues.setPassword(tempChar);

            if (!stateValues.isManagement()) {
                theConsole.printf(MESSAGES.rolesPrompt());
                String userRoles = stateValues.getKnownRoles().get(stateValues.getUserName());
                stateValues.setRoles(theConsole.readLine("[%1$2s]: ", (userRoles == null?"":userRoles)));
            }
        }

        return continuingState;

    }
}

