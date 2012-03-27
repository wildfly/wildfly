/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2011, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.domain.management.security.state;

import org.jboss.as.domain.management.security.ConsoleWrapper;
import org.jboss.sasl.util.UsernamePasswordHashUtil;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.jboss.as.domain.management.security.AddPropertiesUser.NEW_LINE;

public abstract class UpdatePropertiesHandler {

    private ConsoleWrapper theConsole;

    public UpdatePropertiesHandler(ConsoleWrapper theConsole) {
        this.theConsole = theConsole;
    }

    /**
     *  Implement the persistence handler for storing the properties
     */
    abstract void persist(String[] entry, File file) throws IOException;

    /**
     * Customize message for update or add users
     * @param fileName - the filename of the updated property file;
     * @return the console message that should be present to the user
     */
    abstract String consoleUserMessage(String fileName);

    /**
     * Customize message for update or add roles
     * @param fileName - the filename of the updated property file;
     * @return the console message that should be present to the user
     */
    abstract String consoleRolesMessage(String fileName);

    /**
     * Customize error message for update or add users roles / properties
     * @param fileName - the filename of the updated property file;
     * @return the console message that should be present to the user
     */
    abstract String errorMessage(String fileName, Throwable e);

    State update(StateValues stateValues) {
        String[] entry = new String[2];

        try {
            String hash = new UsernamePasswordHashUtil().generateHashedHexURP(stateValues.getUserName(), stateValues.getRealm(),
                    stateValues.getPassword());
            entry[0] = stateValues.getUserName();
            entry[1] = hash;
        } catch (NoSuchAlgorithmException e) {
            return new ErrorState(theConsole, e.getMessage(), null, stateValues);
        }

        for (File current : stateValues.getPropertiesFiles()) {
            try {
                persist(entry, current);
                if (stateValues.isSilent() == false) {
                    theConsole.printf(consoleUserMessage(current.getCanonicalPath()));
                    theConsole.printf(NEW_LINE);
                }
            } catch (Exception e) {
                return new ErrorState(theConsole, errorMessage(current.getAbsolutePath(), e), null, stateValues);
            }
        }

        if (!stateValues.isManagement() && stateValues.getRoles() != null) {
            for (final File current : stateValues.getRoleFiles()) {
                String[] role = {stateValues.getUserName(), stateValues.getRoles()};
                try {
                    persist(role, current);
                    if (stateValues.isSilent() == false) {
                        theConsole.printf(consoleRolesMessage(current.getCanonicalPath()));
                        theConsole.printf(NEW_LINE);
                    }
                } catch (IOException e) {
                    return new ErrorState(theConsole, errorMessage(current.getAbsolutePath(), e), null, stateValues);
                }
            }
        }

        return null;
    }

}
