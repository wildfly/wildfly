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
import static org.jboss.as.domain.management.security.AddPropertiesUser.NEW_LINE;

import org.jboss.as.domain.management.security.ConsoleWrapper;

/**
 * State to prompt the user to choose the name of the realm.
 *
 * For most users the realm should not be modified as it is dependent on being in sync with the core configuration. At a later
 * point it may be possible to split the realm name definition out of the core configuration.
 *
 * This state is only expected to be called when running in interactive mode.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PromptRealmState implements State {

    private final StateValues stateValues;
    private ConsoleWrapper theConsole;

    public PromptRealmState(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    @Override
    public State execute() {
        theConsole.printf(NEW_LINE);
        theConsole.printf(MESSAGES.enterNewUserDetails());
        theConsole.printf(NEW_LINE);

        /*
         * Prompt for realm.
         */
        theConsole.printf(MESSAGES.realmPrompt(stateValues.getRealm()));
        String temp = theConsole.readLine(" : ");
        if (temp == null) {
            /*
             * This will return user to the command prompt so add a new line to ensure the command prompt is on the next line.
             */
            theConsole.printf(NEW_LINE);
            return null;
        }
        if (temp.length() > 0) {
            stateValues.setRealm(temp);
        }

        return new ValidateRealmState(theConsole, stateValues);
    }

}
