/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.web;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

/**
 * Test case to test authentication to web applications using programatic authentication.
 *
 * This test uses an application security-domain mapping to a security domain instead of an authentication factory.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({ WebAuthenticationTestCaseBase.ElytronDomainSetupOverride.class, MinimalWebAuthenticationTestCase.ElytronServletSetupOverride.class })
public class MinimalWebAuthenticationTestCase extends WebAuthenticationTestCaseBase {

    @Override
    protected String getWebXmlName() {
        return "minimal-web.xml";
    }

    public static class ElytronServletSetupOverride extends ServletElytronDomainSetup {

        @Override
        protected boolean useAuthenticationFactory() {
            return false;
        }

    }

}
