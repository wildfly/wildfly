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
import static org.jboss.as.domain.management.security.AddPropertiesUser.DEFAULT_APPLICATION_REALM;
import static org.jboss.as.domain.management.security.AddPropertiesUser.DEFAULT_MANAGEMENT_REALM;

import java.util.Locale;

import org.jboss.as.domain.management.security.AddPropertiesUser;
import org.jboss.as.domain.management.security.ConsoleWrapper;

/**
 * State responsible for asking the user if they are adding a management user or an application user.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class PropertyFilePrompt implements State {

    private ConsoleWrapper theConsole;
    private StateValues stateValues;

    public PropertyFilePrompt(ConsoleWrapper theConsole, StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    @Override
    public State execute() {

        theConsole.printf(AddPropertiesUser.NEW_LINE);
        theConsole.printf(MESSAGES.filePrompt());
        theConsole.printf(AddPropertiesUser.NEW_LINE);

        String temp = theConsole.readLine("(a): ");
        if (temp == null) {
            /*
             * This will return user to the command prompt so add a new line to ensure the command prompt is on the next
             * line.
             */
            theConsole.printf(AddPropertiesUser.NEW_LINE);
            return null;
        }

        switch (convertResponse(temp)) {
            case MANAGEMENT:
                stateValues.setManagement(true);
                stateValues.setRealm(DEFAULT_MANAGEMENT_REALM);
                return new PropertyFileFinder(theConsole, stateValues);
            case APPLICATION:
                stateValues.setManagement(false);
                stateValues.setRealm(DEFAULT_APPLICATION_REALM);
                return new PropertyFileFinder(theConsole, stateValues);
            default:
                return new ErrorState(theConsole, MESSAGES.invalidChoiceResponse(), this);
        }
    }

    private Option convertResponse(final String response) {
        String temp = response.toLowerCase(Locale.ENGLISH);
        if ("".equals(temp) || "a".equals(temp)) {
            return Option.MANAGEMENT;
        }

        if ("b".equals(temp)) {
            return Option.APPLICATION;
        }

        return Option.INVALID;
    }

    private enum Option {
        MANAGEMENT, APPLICATION, INVALID;
    }

}
