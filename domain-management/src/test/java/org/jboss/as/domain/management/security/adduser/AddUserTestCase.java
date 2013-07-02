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

import org.jboss.as.domain.management.security.adduser.AddUser;
import org.jboss.msc.service.StartException;
import org.junit.Test;

import java.io.IOException;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * Test the AddUser state
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class AddUserTestCase extends PropertyTestHelper {

    @Test
    public void testAddUser() throws IOException, StartException {
        values.setRoles(ROLES);
        AddUserState addUserState = new AddUserState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(MESSAGES.addedUser(values.getUserName(), values.getUserFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE).
                expectedDisplayText(MESSAGES.addedRoles(values.getUserName(), values.getRoles(),values.getRoleFiles().get(0).getCanonicalPath())).
                expectedDisplayText(AddUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        addUserState.update(values);

        assertRolePropertyFile(USER_NAME);
        assertUserPropertyFile(USER_NAME);

        consoleBuilder.validate();
    }


}
