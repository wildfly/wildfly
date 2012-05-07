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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.jboss.as.domain.management.security.ConsoleWrapper;
import org.jboss.msc.service.StartException;

/**
 * State to perform the actual addition to the discovered properties files.
 * <p/>
 * By this time ALL validation should be complete, this State will only fail for IOExceptions encountered
 * performing the actual writes.
 */
public class AddUser extends UpdatePropertiesHandler implements State {

    private final StateValues stateValues;
    private final ConsoleWrapper theConsole;

    public AddUser(ConsoleWrapper theConsole, final StateValues stateValues) {
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
            throw new IllegalStateException(MESSAGES.unableToAddUser(file.getAbsolutePath(), e.getMessage()));
        } finally {
            propertiesHandler.stop(null);
        }

    }

    @Override
    String consoleUserMessage(String filePath) {
        return MESSAGES.addedUser(stateValues.getUserName(), filePath);
    }

    @Override
    String consoleRolesMessage(String filePath) {
        return MESSAGES.addedRoles(stateValues.getUserName(), stateValues.getRoles(), filePath);
    }

    @Override
    String errorMessage(String filePath, Throwable e) {
        return MESSAGES.unableToAddUser(filePath, e.getMessage());
    }
}
