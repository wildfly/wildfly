/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.domain.management.security.realms.LdapTestSuite.HOST_NAME;
import static org.jboss.as.domain.management.security.realms.LdapTestSuite.MASTER_LDAP_PORT;
import static org.jboss.as.domain.management.security.realms.LdapTestSuite.SLAVE_LDAP_PORT;

import java.util.List;

import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition.ReferralHandling;
import org.jboss.as.domain.management.security.operations.OutboundConnectionAddBuilder;
import org.jboss.as.domain.management.security.operations.SecurityRealmAddBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * Test case to test group loading where there are referrals.
 *
 * For referrals a lot more combinations need to be tested so multiple connections and realms are defined at once and the
 * individual tests reference the appropriate realm.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class GroupLoadingReferralsSuiteTest extends BaseLdapSuiteTest {

    /*
     * Generic attributes.
     */

    private static final String USERNAME_FILTER = "uid";

    /*
     * Group To Principal Attributes
     */

    // Distinguished Names
    private static final String GROUPS_BASE_DN = "ou=users,dc=group-to-principal,dc=wildfly,dc=org";
    private static final String GROUPS_GROUPS_DN = "ou=groups,dc=group-to-principal,dc=wildfly,dc=org";
    // Realm Names
    static final String GROUP_TO_PRINCIPAL_FOLLOW = "GroupToPrincipalFollow";
    static final String GROUP_TO_PRINCIPAL_THROW = "GroupToPrincipalThrow";

    /*
     * Principal To Group Attributes
     */

    static final String PRINCIPAL_TO_GROUP_FOLLOW_ORIGINAL = "PrincipalToGroupFollowOriginal";
    static final String PRINCIPAL_TO_GROUP_THROW_ORIGINAL = "PrincipalToGroupThrowOriginal";
    static final String PRINCIPAL_TO_GROUP_THROW_REFERRAL = "PrincipalToGroupThrowReferral";

    private static final String PRINCIPAL_BASE_DN = "ou=users,dc=principal-to-group,dc=wildfly,dc=org";


    static final String MASTER_FOLLOW_CONNECTION = "MasterFollow";
    static final String MASTER_THROW_CONNECTION = "MasterThrow";
    static final String SLAVE_CONNECTION = "Slave";



    private SecurityRealmAddBuilder securityRealmBuilder(final String realmName, final String connectionName, final String baseDn) {
        return SecurityRealmAddBuilder.builder(realmName)
                .authentication()
                .ldap()
                .setConnection(connectionName)
                .setBaseDn(baseDn)
                .setUsernameFilter(USERNAME_FILTER)
                .build().build();
    }

    private ModelNode createGroupToPrincipal(final String realmName, final String connectionName) {
        return securityRealmBuilder(realmName, connectionName, GROUPS_BASE_DN)
            .authorization().ldap()
            .setConnection(connectionName)
            .usernameFilter()
            .setBaseDn(GROUPS_BASE_DN)
            .setRecursive(false)
            .setAttribute(USERNAME_FILTER)
            .build()
            .groupToPrincipal()
            .setBaseDn(GROUPS_GROUPS_DN)
            .setPrincipalAttribute("uniqueMember")
            .setIterative(true)
            .setRecursive(true)
            .build().build().build().build();
    }

    private ModelNode createPrincipalToGroup(final String realmName, final String connectionName, final boolean preferOriginalConnection) {
        return securityRealmBuilder(realmName, connectionName, PRINCIPAL_BASE_DN)
            .authorization().ldap()
            .setConnection(connectionName)
            .usernameFilter()
            .setBaseDn(GROUPS_BASE_DN)
            .setRecursive(false)
            .setAttribute(USERNAME_FILTER)
            .build()
            .principalToGroup()
            .setIterative(true)
            .setPreferOriginalConnection(preferOriginalConnection)
            .build()
            .build().build().build();
    }

    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) throws Exception {
        super.addBootOperations(bootOperations);

        /*
         * Add additional security realms.
         */

        bootOperations.add(createGroupToPrincipal(GROUP_TO_PRINCIPAL_FOLLOW, MASTER_FOLLOW_CONNECTION));
        bootOperations.add(createGroupToPrincipal(GROUP_TO_PRINCIPAL_THROW, MASTER_THROW_CONNECTION));

        bootOperations.add(createPrincipalToGroup(PRINCIPAL_TO_GROUP_FOLLOW_ORIGINAL, MASTER_FOLLOW_CONNECTION, true));
        // It is not possible to use FOLLOW and also desire to use the referral connection as without the exception
        // we don't know it was a referral!!
        bootOperations.add(createPrincipalToGroup(PRINCIPAL_TO_GROUP_THROW_ORIGINAL, MASTER_THROW_CONNECTION, true));
        bootOperations.add(createPrincipalToGroup(PRINCIPAL_TO_GROUP_THROW_REFERRAL, MASTER_THROW_CONNECTION, false));

    }

    @Override
    protected void addAddOutboundConnectionOperations(List<ModelNode> bootOperations) throws Exception {
        // Master Follow
        bootOperations.add(OutboundConnectionAddBuilder.builder(MASTER_FOLLOW_CONNECTION)
                .setUrl("ldap://" + HOST_NAME + ":" + MASTER_LDAP_PORT)
                .setSearchDn("uid=wildfly,dc=simple,dc=wildfly,dc=org")
                .setSearchCredential("wildfly_password")
                .setReferrals(ReferralHandling.FOLLOW)
                .build());

        // Master Throw
        bootOperations.add(OutboundConnectionAddBuilder.builder(MASTER_THROW_CONNECTION)
                .setUrl("ldap://" + HOST_NAME + ":" + MASTER_LDAP_PORT)
                // The different DN ensures that FOLLOW would not work from this connection.
                .setSearchDn("uid=master_only,dc=simple,dc=wildfly,dc=org")
                .setSearchCredential("master_password")
                .setReferrals(ReferralHandling.THROW)
                .build());

        // Slave
        bootOperations.add(OutboundConnectionAddBuilder.builder(SLAVE_CONNECTION)
                .setUrl("ldap://" + HOST_NAME + ":" + SLAVE_LDAP_PORT)
                .setSearchDn("uid=wildfly,dc=simple,dc=wildfly,dc=org")
                .setSearchCredential("wildfly_password")
                .addHandlesReferralsFor("ldap://" + HOST_NAME + ":" + SLAVE_LDAP_PORT)
                .addHandlesReferralsFor("ldap://dummy:389")
                .build());

    }

    @Override
    protected void initialiseRealm(SecurityRealmAddBuilder builder) throws Exception {
    }

    // Group To Principal, Follow

    /**
     * Verify that the users group can be loaded when the users distinguished name was discovered after following a referral.
     *
     */
    @Test
    public void groupToPrincipalFollow_User() throws Exception {
        verifyGroupMembership(GROUP_TO_PRINCIPAL_FOLLOW, "ReferralUserSeven", "passwordSeven", "GroupEight");
    }

    // Group To Principal, Throw

    /**
     * Verify that the users group can be loaded when the users distinguished name was discovered after following a referral
     * that was indicated using an exception.
     *
     */
    @Test
    public void groupToPrincipalThrow_User() throws Exception {
        verifyGroupMembership(GROUP_TO_PRINCIPAL_THROW, "ReferralUserSeven", "passwordSeven", "GroupEight");
    }

    // Principal To Group Tests

    @Test
    public void principalToGroupUserGroupReferralGroup() throws Exception {
        verifyGroupMembership(PRINCIPAL_TO_GROUP_THROW_REFERRAL, "TestUserSeven", "passwordSeven", "GroupSeven", "GroupEight");
    }

    @Test
    public void principalToGroupUserReferralGroupGroup() throws Exception {
        verifyGroupMembership(PRINCIPAL_TO_GROUP_THROW_REFERRAL, "ReferralUserEight", "passwordEight", "GroupSeven", "GroupEight");
    }

    @Test
    public void principalToGroupUserReferralGroupOriginal() throws Exception {
        verifyGroupMembership(PRINCIPAL_TO_GROUP_FOLLOW_ORIGINAL, "ReferralUserTen", "passwordTen", "GroupNine");
        verifyGroupMembership(PRINCIPAL_TO_GROUP_THROW_ORIGINAL, "ReferralUserTen", "passwordTen", "GroupNine");
    }

}
