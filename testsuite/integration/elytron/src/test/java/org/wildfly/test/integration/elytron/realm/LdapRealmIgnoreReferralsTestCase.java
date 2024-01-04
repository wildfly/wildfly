/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.realm;

import java.net.URL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.runner.RunWith;

/**
 * <p>Configures referral mode to ignore and executes the smoke tests.</p>
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AbstractLdapRealmTest.LDAPServerSetupTask.class, LdapRealmIgnoreReferralsTestCase.SetupTask.class})
public class LdapRealmIgnoreReferralsTestCase extends AbstractLdapRealmTest {

    @Override
    public void testReferralUser(@ArquillianResource URL webAppURL) throws Exception {
        // the referral user should not be found as referrals are ignored
        assertAuthenticationFailed(webAppURL, USER_REFERRAL, CORRECT_PASSWORD);
    }

    /**
     * SetupTask with referral mode to ignore.
     */
    static class SetupTask extends AbstractLdapRealmTest.SetupTask {
        @Override
        public String getReferralMode() {
            return "ignore";
        }

    }
}
