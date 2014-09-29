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
package org.jboss.as.domain.management.security.realms;

import org.jboss.as.domain.management.security.operations.SecurityRealmAddBuilder;
import org.jboss.as.domain.management.security.operations.CacheBuilder.By;
import org.junit.Test;

/**
 * A test suite test to test LDAP based group assignment where principals contain the memberOf attribute referencing the groups
 * that they are a member of.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PrincipalToGroupLdapSuiteTest extends LdapGroupAssignmentBaseSuiteTest {

    private static final String BASE_DN = "ou=users,dc=principal-to-group,dc=wildfly,dc=org";
    private static final String USERNAME_FILTER = "uid";

    @Override
    protected void initialiseRealm(SecurityRealmAddBuilder builder) throws Exception {
        builder.authentication()
        .ldap()
        .setConnection(MASTER_CONNECTION_NAME)
        .setBaseDn(BASE_DN)
        .setUsernameFilter(USERNAME_FILTER)
        .cache()
        .setBy(By.SEARCH_TIME)
        .setEvictionTime(1)
        .setMaxCacheSize(1)
        .build().build().build()
        .authorization().ldap()
        .setConnection(MASTER_CONNECTION_NAME)
        .usernameFilter()
        .setBaseDn(BASE_DN)
        .setRecursive(false)
        .setAttribute(USERNAME_FILTER)
        .cache()
        .setBy(By.ACCESS_TIME)
        .setEvictionTime(1)
        .setMaxCacheSize(1)
        .build().build()
        .principalToGroup()
        .setIterative(true)
        .setSkipMissingGroups(true)
        .cache()
        .setBy(By.SEARCH_TIME)
        .setEvictionTime(1)
        .setMaxCacheSize(1)
        .build().build().build().build();
    }

    /**
     * Expected membership (GroupTwo)
     *
     * This user is also a member of a missing group which should be ignored.
     */
    @Test
    public void testTestUserEleven() throws Exception {
        verifyGroupMembership(TEST_REALM, "TestUserEleven", "passwordEleven", "GroupTwo");
    }

}
