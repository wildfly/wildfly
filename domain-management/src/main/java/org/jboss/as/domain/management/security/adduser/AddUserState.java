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

package org.jboss.as.domain.management.security.adduser;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * State to perform the actual addition to the discovered properties files.
 * <p/>
 * By this time ALL validation should be complete, this State will only fail for IOExceptions encountered
 * performing the actual writes.
 */
public class AddUserState extends UpdatePropertiesHandler implements State {

    private final StateValues stateValues;
    private final ConsoleWrapper theConsole;

    public AddUserState(ConsoleWrapper theConsole, final StateValues stateValues) {
        super(theConsole);
        this.theConsole = theConsole;
        this.stateValues = stateValues;
    }

    @Override
    public State execute() {
        final String password = stateValues.getPassword();
        State nextState;
        if (password == null) {
            // The user doesn't exist and the password is not provided !
            nextState = new ErrorState(theConsole, MESSAGES.noPasswordExiting(), null, stateValues);
        } else {
            nextState = update(stateValues);
        }

        /*
         * If this is interactive mode and no error occurred offer to display the
         * Base64 password of the user - otherwise the util can end.
         */
        if (nextState == null && stateValues.isInteractive()) {
            nextState = new ConfirmationChoice(theConsole, MESSAGES.serverUser(), MESSAGES.yesNo(), new DisplaySecret(
                    theConsole, stateValues), null);

        }
        return nextState;
    }

    @Override
    String consoleUserMessage(String filePath) {
        return MESSAGES.addedUser(stateValues.getUserName(), filePath);
    }

    @Override
    String consoleGroupsMessage(String filePath) {
        return MESSAGES.addedGroups(stateValues.getUserName(), stateValues.getGroups(), filePath);
    }

    @Override
    String errorMessage(String filePath, Throwable e) {
        return MESSAGES.unableToAddUser(filePath, e.getMessage());
    }
}
