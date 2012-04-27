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
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * Test the duplicated user check state
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DuplicateUserCheckStateTestCase extends PropertyTestHelper {

    @Test
    public void newUser() throws IOException {
        values.setExistingUser(false);
        values.setRoles(ROLES);
        DuplicateUserCheckState userCheckState = new DuplicateUserCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.aboutToAddUser(values.getUserName(), values.getRealm())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE).
                expectedDisplayText(MESSAGES.isCorrectPrompt()).
                expectedDisplayText(" ").
                expectedInput("yes").
                expectedDisplayText(MESSAGES.addedUser(values.getUserName(), values.getPropertiesFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE).
                expectedDisplayText(MESSAGES.addedRoles(values.getUserName(), values.getRoles(),values.getRoleFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);

        State nextState = userCheckState.execute();
        assertTrue(nextState instanceof ConfirmationChoice);
        nextState = nextState.execute();
        assertTrue(nextState instanceof AddUser);
        nextState.execute();
        consoleBuilder.validate();
    }

    @Test
    public void existingUSer() throws IOException {
        values.setExistingUser(true);
        values.setRoles(ROLES);
        DuplicateUserCheckState userCheckState = new DuplicateUserCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.updateUser(values.getUserName(), values.getPropertiesFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE).
                expectedDisplayText(MESSAGES.updatedRoles(values.getUserName(), values.getRoles(), values.getRoleFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);

        State nextState = userCheckState.execute();
        assertTrue(nextState instanceof UpdateUser);
        nextState = nextState.execute();
        consoleBuilder.validate();
    }

}
