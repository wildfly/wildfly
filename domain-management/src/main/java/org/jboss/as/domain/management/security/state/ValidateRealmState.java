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

import static org.jboss.as.domain.management.security.AddPropertiesUser.DEFAULT_MANAGEMENT_REALM;
import static org.jboss.as.domain.management.security.AddPropertiesUser.DEFAULT_APPLICATION_REALM;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import org.jboss.as.domain.management.security.ConsoleWrapper;

/**
 * State to perform some validation in the entered realm.
 *
 * Primarily this is just to warn users who have chosen a different realm name.
 *
 * This state is only expected to be used in interactive mode.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ValidateRealmState implements State {

    private final StateValues stateValues;
    private ConsoleWrapper theConsole;

    public ValidateRealmState(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    @Override
    public State execute() {
        final String expectedRealm = stateValues.isManagement() ? DEFAULT_MANAGEMENT_REALM : DEFAULT_APPLICATION_REALM;
        String enteredRealm = stateValues.getRealm();

        if (expectedRealm.equals(enteredRealm) == false) {
            String message = MESSAGES.alternativeRealm(expectedRealm);
            String prompt = MESSAGES.realmConfirmation(enteredRealm) + " " + MESSAGES.yes() + "/" + MESSAGES.no() + "?";

            return new ConfirmationChoice(theConsole, message, prompt, new PromptNewUserState(theConsole, stateValues),
                    new PromptRealmState(theConsole, stateValues));

        }

        return new PromptNewUserState(theConsole, stateValues);
    }

}
