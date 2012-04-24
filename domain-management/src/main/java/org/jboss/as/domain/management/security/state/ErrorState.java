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
import static org.jboss.as.domain.management.security.AddPropertiesUser.NEW_LINE;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * State to report an error to the user, optionally a nextState can be supplied so the process can continue even though an
 * error has been reported.
 */
public class ErrorState implements State {

    private final State nextState;
    private final String errorMessage;
    private final StateValues stateValues;
    private ConsoleWrapper theConsole;

    public ErrorState(ConsoleWrapper theConsole, String errorMessage) {
        this(theConsole, errorMessage, null, null);
    }

    public ErrorState(ConsoleWrapper theConsole, String errorMessage, State nextState) {
        this(theConsole, errorMessage, nextState, null);
    }

    public ErrorState(ConsoleWrapper theConsole, String errorMessage, State nextState, StateValues stateValues) {
        this.errorMessage = errorMessage;
        this.nextState = nextState;
        this.stateValues = stateValues;
        this.theConsole = theConsole;
        if ((stateValues != null && stateValues.isSilent() == false) && theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    @Override
    public State execute() {
        if ((stateValues == null) || (stateValues != null) && (stateValues.isSilent() == false)) {
            theConsole.printf(NEW_LINE);
            theConsole.printf(" * ");
            theConsole.printf(MESSAGES.errorHeader());
            theConsole.printf(" * ");
            theConsole.printf(NEW_LINE);

            theConsole.printf(errorMessage);
            theConsole.printf(NEW_LINE);
            theConsole.printf(NEW_LINE);
        }
        return nextState;
    }

}
