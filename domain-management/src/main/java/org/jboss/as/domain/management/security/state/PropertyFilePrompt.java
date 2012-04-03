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

import org.jboss.as.domain.management.security.AddPropertiesUser;
import org.jboss.as.domain.management.security.ConsoleWrapper;
import static org.jboss.as.domain.management.security.AddPropertiesUser.DEFAULT_MANAGEMENT_REALM;
import static org.jboss.as.domain.management.security.AddPropertiesUser.DEFAULT_APPLICATION_REALM;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
* Describe the purpose
*
* @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
*/
public class PropertyFilePrompt implements State {

    private static final int MANAGEMENT = 0;
    private static final int APPLICATION = 1;
    private static final int INVALID = 2;

    private ConsoleWrapper theConsole;

    public PropertyFilePrompt(ConsoleWrapper theConsole) {
        this.theConsole = theConsole;
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    @Override
    public State execute() {
        StateValues stateValues = new StateValues();

        theConsole.printf(AddPropertiesUser.NEW_LINE);
        theConsole.printf(MESSAGES.filePrompt());
        theConsole.printf(AddPropertiesUser.NEW_LINE);

        while (true) {
            String temp = theConsole.readLine("(a): ");
            if (temp == null) {
                /*
                 * This will return user to the command prompt so add a new line to ensure the command prompt is on the next
                 * line.
                 */
                theConsole.printf(AddPropertiesUser.NEW_LINE);
                return null;
            }

            if (temp.length() > 0) {
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
            } else {
                stateValues.setManagement(true);
                stateValues.setRealm(DEFAULT_MANAGEMENT_REALM);
                return new PropertyFileFinder(theConsole, stateValues);
            }
        }
    }

    private int convertResponse(final String response) {
        String temp = response.toLowerCase();
        if ("A".equals(temp) || "a".equals(temp)) {
            return MANAGEMENT;
        }

        if ("B".equals(temp) || "b".equals(temp)) {
            return APPLICATION;
        }

        return INVALID;
    }

}
