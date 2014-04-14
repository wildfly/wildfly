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

    private static final String BASE_DN = "ou=users,dc=group-to-principal,dc=wildfly,dc=org";
    private static final String GROUPS_DN = "ou=groups,dc=group-to-principal,dc=wildfly,dc=org";
    private static final String USERNAME_FILTER = "uid";

    static final String MASTER_FOLLOW_CONNECTION = "MasterFollow";
    static final String MASTER_THROW_CONNECTION = "MasterThrow";
    static final String SLAVE_CONNECTION = "Slave";

    static final String GROUP_TO_PRINCIPAL_FOLLOW = "GroupToPrincipalFollow";
    static final String GROUP_TO_PRINCIPAL_THROW = "GroupToPrincipalThrow";

    private ModelNode createGroupToPrincipal(final String realmName, final String connectionName) {
        return SecurityRealmAddBuilder.builder(realmName)
            .authentication()
            .ldap()
            .setConnection(connectionName)
            .setBaseDn(BASE_DN)
            .setUsernameFilter(USERNAME_FILTER)
            .build().build()
            .authorization().ldap()
            .setConnection(connectionName)
            .usernameFilter()
            .setBaseDn(BASE_DN)
            .setRecursive(false)
            .setAttribute(USERNAME_FILTER)
            .build()
            .groupToPrincipal()
            .setBaseDn(GROUPS_DN)
            .setPrincipalAttribute("uniqueMember")
            .setIterative(true)
            .setRecursive(true)
            .build().build().build().build();
    }

    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) throws Exception {
        super.addBootOperations(bootOperations);

        /*
         * Add additional security realms.
         */

        bootOperations.add(createGroupToPrincipal(GROUP_TO_PRINCIPAL_FOLLOW, MASTER_FOLLOW_CONNECTION));
        bootOperations.add(createGroupToPrincipal(GROUP_TO_PRINCIPAL_THROW, MASTER_THROW_CONNECTION));
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

}
