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
import org.jboss.msc.service.StartException;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * Describe the purpose
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class UpdateUser extends UpdatePropertiesHandler implements State {

    private final StateValues stateValues;
    private final ConsoleWrapper theConsole;

    public UpdateUser(ConsoleWrapper theConsole,final StateValues stateValues) {
        super(theConsole);
        this.theConsole = theConsole;
        this.stateValues = stateValues;
    }

    @Override
    public State execute() {
        State nextState = update(stateValues);
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
    void persist(String[] entry, File file) throws IOException {
        UserPropertiesFileHandler propertiesHandler = new UserPropertiesFileHandler(file.getAbsolutePath());
        try {
            propertiesHandler.start(null);
            Properties prob = propertiesHandler.getProperties();
            prob.setProperty(entry[0], entry[1]);
            propertiesHandler.persistProperties();
        } catch (StartException e) {
            throw new IllegalStateException(MESSAGES.unableToUpdateUser(file.getAbsolutePath(), e.getMessage()));
        } finally {
            propertiesHandler.stop(null);
        }

    }

    @Override
    String consoleUserMessage(String fileName) {
        return MESSAGES.updateUser(stateValues.getUserName(), fileName);
    }

    @Override
    String consoleRolesMessage(String fileName) {
        return MESSAGES.updatedRoles(stateValues.getUserName(), stateValues.getRoles(), fileName);
    }

    @Override
    String errorMessage(String fileName, Throwable e) {
        return MESSAGES.unableToUpdateUser(fileName, e.getMessage());
    }
}


