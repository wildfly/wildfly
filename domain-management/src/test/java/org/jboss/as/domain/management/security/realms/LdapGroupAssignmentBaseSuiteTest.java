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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Different LDAP definitions are used to test different approaches to group assignment within LDAP.
 *
 * Despite the different structures the end result of a user's group membership remains the same so this class performs the same
 * set of tests on the supplied realm.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class LdapGroupAssignmentBaseSuiteTest extends BaseLdapSuiteTest {

    private static boolean initialised;

    @BeforeClass
    public static void startLdapServer() throws Exception {
        initialised = LdapTestSuite.startLdapServers(false);
    }

    @AfterClass
    public static void stopLdapServer() throws Exception {
        if (initialised) {
            LdapTestSuite.stopLdapServers();
        }
    }

    /**
     * Expected membership (CycleGroupA, CycleGroupB, CycleGroupC)
     */
    @Test
    public void testCycleUser() throws Exception {
        verifyGroupMembership(TEST_REALM, "cycleUser", "passwordCycle", "CycleGroupA", "CycleGroupB", "CycleGroupC");
    }

    /**
     * Expected membership (GroupOne, GroupFive)
     */
    @Test
    public void testTestUserOne() throws Exception {
        verifyGroupMembership(TEST_REALM, "TestUserOne", "passwordOne", "GroupOne", "GroupFive");
    }

    /**
     * Expected membership (GroupTwo)
     */
    @Test
    public void testTestUserTwo() throws Exception {
        verifyGroupMembership(TEST_REALM, "TestUserTwo", "passwordTwo", "GroupTwo");
    }

    /**
     * Expected membership (GroupThree, GroupFour)
     */
    @Test
    public void testTestUserThree() throws Exception {
        verifyGroupMembership(TEST_REALM, "TestUserThree", "passwordThree", "GroupThree", "GroupFour");
    }

    /**
     * Expected membership (GroupFour)
     */
    @Test
    public void testTestUserFour() throws Exception {
        verifyGroupMembership(TEST_REALM, "TestUserFour", "passwordFour", "GroupFour");
    }

    /**
     * Expected membership (GroupFive)
     */
    @Test
    public void testTestUserFive() throws Exception {
        verifyGroupMembership(TEST_REALM, "TestUserFive", "passwordFive", "GroupFive");
    }

    /**
     * Expected membership (GroupSix, GroupOne)
     */
    @Test
    public void testTestUserSix() throws Exception {
        verifyGroupMembership(TEST_REALM, "TestUserSix", "passwordSix", "GroupSix", "GroupTwo");
    }

}
