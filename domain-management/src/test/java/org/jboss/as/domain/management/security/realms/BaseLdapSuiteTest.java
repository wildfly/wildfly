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

import static org.jboss.as.domain.management.security.realms.LdapTestSuite.HOST_NAME;
import static org.jboss.as.domain.management.security.realms.LdapTestSuite.LDAP_PORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.core.security.RealmGroup;
import org.jboss.as.core.security.SimplePrincipal;
import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.AuthorizingCallbackHandler;
import org.jboss.as.domain.management.security.operations.OutboundConnectionAddBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * A base class for all LDAP test to allow the server to be initialised if
 * being executed outside of the suite.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class BaseLdapSuiteTest extends SecurityRealmTestBase {

    protected static final String CONNECTION_NAME = "TestConnection";
    private static boolean initialised;

    @BeforeClass
    public static void startLdapServer() throws Exception {
        initialised = LdapTestSuite.startLdapServer();
    }

    @AfterClass
    public static void stopLdapServer() throws Exception {
        if (initialised) {
            LdapTestSuite.stopLdapServer();
        }
    }

    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) throws Exception {
        bootOperations.add(OutboundConnectionAddBuilder.builder(CONNECTION_NAME)
                .setUrl("ldap://" + HOST_NAME + ":" + LDAP_PORT)
                .setSearchDn("uid=wildfly,dc=simple,dc=wildfly,dc=org")
                .setSearchCredential("wildfly_password")
                .build());

        super.addBootOperations(bootOperations);
    }

    private AuthorizingCallbackHandler getAuthorizingCallbackHandler() {
        return securityRealm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN);
    }

    private Set<RealmGroup> getUsersGroups(final String userName, final String password) throws Exception {
        AuthorizingCallbackHandler cbh = getAuthorizingCallbackHandler();

        NameCallback ncb = new NameCallback("Username", userName);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback(password);

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertTrue("Password verified", vpc.isVerified());

        Principal user = new SimplePrincipal(userName);
        Collection<Principal> principals = Collections.singleton(user);
        SubjectUserInfo userInfo = cbh.createSubjectUserInfo(principals);

        return userInfo.getSubject().getPrincipals(RealmGroup.class);
    }

    protected void verifyGroupMembership(final String userName, final String password, final String... groups) throws Exception {
        Set<RealmGroup> groupPrincipals = getUsersGroups(userName, password);
        assertEquals("Number of groups", groups.length, groupPrincipals.size());
        Collection<String> expectedGroups = Arrays.asList(groups);
        for (RealmGroup current : groupPrincipals) {
            assertTrue(String.format("User not expected to be in group '%s'", current.getName()),
                    expectedGroups.remove(current.getName()));
        }
        assertTrue(String.format("User not in expected groups '%s'", expectedGroups.toString()), expectedGroups.isEmpty());
    }

}
