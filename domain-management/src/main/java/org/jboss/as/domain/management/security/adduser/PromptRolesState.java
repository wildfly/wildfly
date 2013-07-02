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
package org.jboss.as.domain.management.security.adduser;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * State responsible for prompting for the list of roles for a user.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PromptRolesState implements State {

    private final StateValues stateValues;
    private final ConsoleWrapper theConsole;

    public PromptRolesState(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
    }

    @Override
    public State execute() {
        if (stateValues.isSilentOrNonInteractive() == false) {
            if (!stateValues.getRoleFiles().isEmpty()) {
                theConsole.printf(MESSAGES.rolesPrompt());
                String userRoles = stateValues.getKnownRoles().get(stateValues.getUserName());
                stateValues.setRoles(theConsole.readLine("[%1$2s]: ", (userRoles == null ? "" : userRoles)));
            }
        }

        return new PreModificationState(theConsole, stateValues);
    }

}
