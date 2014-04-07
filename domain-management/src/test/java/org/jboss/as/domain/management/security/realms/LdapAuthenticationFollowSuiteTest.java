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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.core.security.RealmUser;
import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.AuthorizingCallbackHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.operations.SecurityRealmAddBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.junit.Test;

/**
 * Test case to test authentication when the referral mode is FOLLOW.
 *
 * Tests both following referrals from the master to the slave and also directly authenticating against the slave.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapAuthenticationFollowSuiteTest extends BaseLdapSuiteReferralsTest {

    private static final String SLAVE_REALM = "SlaveRealm";
    private static final String BASE_DN = "dc=simple,dc=wildfly,dc=org";
    private static final String USERNAME_FILTER = "uid";

    private static final String USER_THREE_DIRECT = "user_three";
    private static final String USER_THREE_REFERRAL = "referral_user_three";
    private static final String USER_THREE_PASSWORD = "three_password";

    /*
     * Test the realm using the slave server directly.
     */

    private AuthorizingCallbackHandler getSlaveCallbackHandler() {
        return ((SecurityRealm) getContainer().getService(SecurityRealm.ServiceUtil.createServiceName(SLAVE_REALM))
                .getValue()).getAuthorizingCallbackHandler(AuthMechanism.PLAIN);
    }

    @Test
    public void testValidUserDirect() throws Exception {
        AuthorizingCallbackHandler cbh = getSlaveCallbackHandler();

        NameCallback ncb = new NameCallback("Username", USER_THREE_DIRECT);
        RealmCallback rcb = new RealmCallback("Realm", SLAVE_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback(USER_THREE_PASSWORD);

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertTrue("Password Verified", vpc.isVerified());
    }

    @Test
    public void testBadPasswordDirect() throws Exception {
        AuthorizingCallbackHandler cbh = getSlaveCallbackHandler();

        NameCallback ncb = new NameCallback("Username", USER_THREE_DIRECT);
        RealmCallback rcb = new RealmCallback("Realm", SLAVE_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback("BAD");

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertFalse("Password Not Verified", vpc.isVerified());
    }

    /*
     * Test Authentication Following A Referral
     */

    @Test
    public void testVerifyGoodPasswordFollow() throws Exception {
        AuthorizingCallbackHandler cbh = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN);

        NameCallback ncb = new NameCallback("Username", USER_THREE_REFERRAL);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback(USER_THREE_PASSWORD);

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertTrue("Password Verified", vpc.isVerified());

        Collection<Principal> principals = Collections.emptyList();
        SubjectUserInfo userInfo = cbh.createSubjectUserInfo(principals);
        principals = userInfo.getPrincipals();
        assertEquals("Principal Count", 1, principals.size());
        RealmUser user = (RealmUser) principals.iterator().next();
        assertEquals("Expected Username", "user_three", user.getName());
    }

    @Test
    public void testVerifyBadPasswordFollow() throws Exception {
        AuthorizingCallbackHandler cbh = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN);

        NameCallback ncb = new NameCallback("Username", USER_THREE_REFERRAL);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback("BAD");

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertFalse("Password Not Verified", vpc.isVerified());
    }

    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) throws Exception {
        super.addBootOperations(bootOperations);

        // We define a second realm here as well.
        bootOperations.add(SecurityRealmAddBuilder.builder(SLAVE_REALM)
                .authentication().ldap()
                .setConnection(SLAVE_CONNECTION_NAME)
                .setBaseDn(BASE_DN)
                .setUsernameFilter(USERNAME_FILTER)
                .build().build().build());
    }

    @Override
    protected void initialiseRealm(SecurityRealmAddBuilder builder) throws Exception {
        builder.authentication()
                .ldap()
                .setConnection(MASTER_CONNECTION_NAME)
                .setBaseDn(BASE_DN)
                .setUsernameFilter(USERNAME_FILTER)
                .setUsernameLoad(USERNAME_FILTER)
                .build().build();
    }

}
