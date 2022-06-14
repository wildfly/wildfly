/*
 *  Copyright (c) 2022 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
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
