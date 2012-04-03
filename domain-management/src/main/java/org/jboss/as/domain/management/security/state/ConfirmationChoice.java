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
import static org.jboss.as.domain.management.security.AddPropertiesUser.NEW_LINE;
import static org.jboss.as.domain.management.security.AddPropertiesUser.SPACE;

/**
 * State to display a message to the user with option to confirm a choice.
 * <p/>
 * This state handles either a yes or no outcome and will loop with an error
 * on invalid input.
 */
public class ConfirmationChoice implements State {

    private ConsoleWrapper theConsole;
    private final String message;
    private final String prompt;
    private final State yesState;
    private final State noState;

    private static final int YES = 0;
    private static final int NO = 1;
    private static final int INVALID = 2;

    public ConfirmationChoice(ConsoleWrapper theConsole,final String message, final String prompt, final State yesState, final State noState) {
        this.theConsole = theConsole;
        this.message = message;
        this.prompt = prompt;
        this.yesState = yesState;
        this.noState = noState;
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
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
                return new ErrorState(theConsole, MESSAGES.invalidConfirmationResponse(), this);
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
