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

import org.jboss.as.domain.management.logging.DomainManagementLogger;

/**
 * State responsible for prompting for the list of groups for a user.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PromptGroupsState implements State {

    private final StateValues stateValues;
    private final ConsoleWrapper theConsole;

    public PromptGroupsState(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
    }

    @Override
    public State execute() {
        if (stateValues.isSilentOrNonInteractive() == false) {
            if (!stateValues.getGroupFiles().isEmpty()) {
                theConsole.printf(DomainManagementLogger.ROOT_LOGGER.groupsPrompt());
                String userGroups = stateValues.getKnownGroups().get(stateValues.getUserName());
                stateValues.setGroups(theConsole.readLine("[%1$2s]: ", (userGroups == null ? "" : userGroups)));
            }
        }

        return new PreModificationState(theConsole, stateValues);
    }

}
