/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import org.jboss.as.domain.management.security.AssertConsoleBuilder;
import org.jboss.msc.service.StartException;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test update user
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class UpdateUserTestCase extends PropertyTestHelper {

    @Test
    public void testState() throws IOException, StartException {
        values.setRoles(null);
        UpdateUser updateUserState = new UpdateUser(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(updateUserState.consoleUserMessage(values.getPropertiesFiles().get(0).getAbsolutePath())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        updateUserState.update(values);

        assertUserPropertyFile();

        consoleBuilder.validate();
    }

    @Test
    public void testStateRoles() throws IOException, StartException {
        values.setRoles(ROLES);
        UpdateUser updateUserState = new UpdateUser(consoleMock, values);
        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(updateUserState.consoleUserMessage(values.getPropertiesFiles().get(0).getAbsolutePath())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE).
                expectedDisplayText(updateUserState.consoleRolesMessage(values.getRoleFiles().get(0).getAbsolutePath())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        updateUserState.update(values);

        assertUserPropertyFile();
        assertRolePropertyFile();

        consoleBuilder.validate();
    }

    private void assertUserPropertyFile() throws StartException, IOException {
        Properties properties = loadProperties(values.getPropertiesFiles().get(0).getAbsolutePath());
        String password = (String) properties.get(USER_NAME);
        assertNotNull(password);
    }

    private void assertRolePropertyFile() throws StartException, IOException {
        Properties properties = loadProperties(values.getRoleFiles().get(0).getAbsolutePath());
        String roles = (String) properties.get(USER_NAME);
        assertEquals(ROLES,roles);
    }


}
