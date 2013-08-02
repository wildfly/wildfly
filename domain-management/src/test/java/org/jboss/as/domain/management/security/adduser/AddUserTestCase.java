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

package org.jboss.as.domain.management.security.adduser;

import org.jboss.msc.service.StartException;
import org.junit.Test;

import java.io.IOException;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test the AddUser state
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class AddUserTestCase extends PropertyTestHelper {

    @Test
    public void testAddUser() throws IOException, StartException {
        values.setGroups(ROLES);
        AddUserState addUserState = new AddUserState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.addedUser(values.getUserName(), values.getUserFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE).
                expectedDisplayText(MESSAGES.addedGroups(values.getUserName(), values.getGroups(), values.getGroupFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        addUserState.update(values);

        assertRolePropertyFile(values.getUserName());
        assertUserPropertyFile(values.getUserName());

        consoleBuilder.validate();
    }


    @Test
    public void testAddEnabledUser() throws IOException, StartException {
        values.setUserName("Donny.Donowitz");
        values.setGroups(ROLES);
        values.getOptions().setDisable(false);
        AddUserState addUserState = new AddUserState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.addedUser(values.getUserName(), values.getUserFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE).
                expectedDisplayText(MESSAGES.addedGroups(values.getUserName(), values.getGroups(), values.getGroupFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        addUserState.update(values);

        assertRolePropertyFile(values.getUserName());
        assertUserPropertyFile(values.getUserName());

        consoleBuilder.validate();
    }

    @Test
    public void testAddDisabledUser() throws IOException, StartException {
        values.setGroups(ROLES);
        values.getOptions().setDisable(true);
        values.setUserName("Omar.Ulmer");
        AddUserState addUserState = new AddUserState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.addedUser(values.getUserName(), values.getUserFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE).
                expectedDisplayText(MESSAGES.addedGroups(values.getUserName(), values.getGroups(), values.getGroupFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        addUserState.update(values);

        assertNull("The user is disabled, the user line must start with #", getPasswordFromUserProperty(values.getUserName()));
        assertNull("The user is disabled, the roles line must start with #", getRolesFromRoleProperty(values.getUserName()));
        assertRolePropertyFile(values.getUserName(), true);
        assertUserPropertyFile(values.getUserName(), true);

        consoleBuilder.validate();
    }

    @Test
    public void testEnableDisabledUser() throws IOException, StartException {
        // Disable user
        values.setGroups(ROLES);
        values.getOptions().setDisable(true);
        values.setUserName("Omar.Ulmer");
        AddUserState addUserState = new AddUserState(consoleMock, values);
        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.addedUser(values.getUserName(), values.getUserFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE).
                expectedDisplayText(MESSAGES.addedGroups(values.getUserName(), values.getGroups(), values.getGroupFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        addUserState.update(values);

        assertNull("The user is disabled, the user line must start with #", getPasswordFromUserProperty(values.getUserName()));
        assertNull("The user is disabled, the roles line must start with #", getRolesFromRoleProperty(values.getUserName()));
        assertRolePropertyFile(values.getUserName(), true);
        assertUserPropertyFile(values.getUserName(), true);
        consoleBuilder.validate();

        int roleFileLineNumber = countLineNumberRoleFile();
        int userFileLineNumber = countLineNumberUserFile();

        // Enable user
        values.getOptions().setDisable(false);
        consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.addedUser(values.getUserName(), values.getUserFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE).
                expectedDisplayText(MESSAGES.addedGroups(values.getUserName(), values.getGroups(), values.getGroupFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        addUserState = new AddUserState(consoleMock, values);
        addUserState.update(values);

        assertRolePropertyFile(values.getUserName());
        assertUserPropertyFile(values.getUserName());
        assertEquals("Enabling a role just uncomment the line and must not create a new one", roleFileLineNumber, countLineNumberRoleFile());
        assertEquals("Enabling a user just uncomment the line and must not create a new one", userFileLineNumber, countLineNumberUserFile());
        consoleBuilder.validate();
    }

    @Test
    public void testEnableEnabledUser() throws IOException, StartException {
        // Enable user
        values.setGroups(ROLES);
        values.getOptions().setDisable(false);
        values.setUserName("Aldo.Raine");
        AddUserState addUserState = new AddUserState(consoleMock, values);
        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.addedUser(values.getUserName(), values.getUserFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE).
                expectedDisplayText(MESSAGES.addedGroups(values.getUserName(), values.getGroups(), values.getGroupFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        addUserState.update(values);

        assertRolePropertyFile(values.getUserName());
        assertUserPropertyFile(values.getUserName());
        consoleBuilder.validate();

        int roleFileLineNumber = countLineNumberRoleFile();
        int userFileLineNumber = countLineNumberUserFile();

        // (Re)Enable user
        values.getOptions().setDisable(false);
        consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.addedUser(values.getUserName(), values.getUserFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE).
                expectedDisplayText(MESSAGES.addedGroups(values.getUserName(), values.getGroups(), values.getGroupFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        addUserState = new AddUserState(consoleMock, values);
        addUserState.update(values);

        assertRolePropertyFile(values.getUserName());
        assertUserPropertyFile(values.getUserName());
        assertEquals("Enabling a role just uncomment the line and must not create a new one", roleFileLineNumber, countLineNumberRoleFile());
        assertEquals("Enabling a user just uncomment the line and must not create a new one", userFileLineNumber, countLineNumberUserFile());
        consoleBuilder.validate();
    }
}
