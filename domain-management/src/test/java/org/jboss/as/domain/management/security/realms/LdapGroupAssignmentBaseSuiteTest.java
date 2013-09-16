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

    /**
     * Expected membership (CycleGroupA, CycleGroupB, CycleGroupC)
     */
    @Test
    public void testCycleUser() throws Exception {
        verifyGroupMembership("cycleUser", "passwordCycle", "CycleGroupA", "CycleGroupB", "CycleGroupC");
    }

    /**
     * Expected membership (GroupOne, GroupFive)
     */
    @Test
    public void testTestUserOne() throws Exception {
        verifyGroupMembership("TestUserOne", "passwordOne", "GroupOne", "GroupFive");
    }

    /**
     * Expected membership (GroupTwo)
     */
    @Test
    public void testTestUserTwo() throws Exception {
        verifyGroupMembership("TestUserTwo", "passwordTwo", "GroupTwo");
    }

    /**
     * Expected membership (GroupThree, GroupFour)
     */
    @Test
    public void testTestUserThree() throws Exception {
        verifyGroupMembership("TestUserThree", "passwordThree", "GroupThree", "GroupFour");
    }

    /**
     * Expected membership (GroupFour)
     */
    @Test
    public void testTestUserFour() throws Exception {
        verifyGroupMembership("TestUserFour", "passwordFour", "GroupFour");
    }

    /**
     * Expected membership (GroupFive)
     */
    @Test
    public void testTestUserFive() throws Exception {
        verifyGroupMembership("TestUserFive", "passwordFive", "GroupFive");
    }

    /**
     * Expected membership (GroupSix, GroupOne)
     */
    @Test
    public void testTestUserSix() throws Exception {
        verifyGroupMembership("TestUserSix", "passwordSix", "GroupSix", "GroupOne");
    }

}
