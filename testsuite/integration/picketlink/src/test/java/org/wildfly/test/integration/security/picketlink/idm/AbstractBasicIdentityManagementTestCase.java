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

package org.wildfly.test.integration.security.picketlink.idm;

import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.picketlink.idm.model.basic.BasicModel.getRole;
import static org.picketlink.idm.model.basic.BasicModel.getUser;
import static org.picketlink.idm.model.basic.BasicModel.hasRole;

/**
 * <p>Base test class covering basic scenarios.</p>
 *
 * @author Pedro Igor
 */
public abstract class AbstractBasicIdentityManagementTestCase {

    @Test
    @InSequence(1)
    public void testPartitionManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        Realm partition = partitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);

        if (partition != null) {
            partitionManager.remove(partition);
        }

        partitionManager.add(new Realm(Realm.DEFAULT_REALM));

        assertNotNull(partitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM));
    }

    @Test
    @InSequence(2)
    public void testUserManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        String loginName = "johny";
        User user = getUser(identityManager, loginName);

        if (user != null) {
            identityManager.remove(user);
        }

        identityManager.add(new User(loginName));

        assertNotNull(getUser(identityManager, loginName));
    }

    @Test
    @InSequence(3)
    public void testCredentialManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        User user = getUser(identityManager, "johny");
        Password password = new Password("abcd1234");

        identityManager.updateCredential(user, password);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);

        identityManager.validateCredentials(credentials);

        assertEquals(Credentials.Status.VALID, credentials.getStatus());
    }

    @Test
    @InSequence(4)
    public void testRoleManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        String roleName = "admin";
        Role role = getRole(identityManager, roleName);

        if (role != null) {
            identityManager.remove(role);
        }

        identityManager.add(new Role(roleName));

        assertNotNull(getRole(identityManager, roleName));
    }

    @Test
    @InSequence(5)
    public void testRelationshipManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        User user = getUser(identityManager, "johny");
        Role role = getRole(identityManager, "admin");

        RelationshipManager relationshipManager = partitionManager.createRelationshipManager();

        BasicModel.grantRole(relationshipManager, user, role);

        assertTrue(hasRole(relationshipManager, user, role));
    }

    @Test
    @InSequence(6)
    public void testAttributeManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        User user = getUser(identityManager, "johny");

        assertNull(user.getAttribute("testAttribute"));

        user.setAttribute(new Attribute<String>("testAttribute", "value"));

        identityManager.update(user);

        assertNotNull(user.getAttribute("testAttribute"));
        assertEquals("value", user.getAttribute("testAttribute").getValue());
    }

    protected abstract PartitionManager getPartitionManager();
}
