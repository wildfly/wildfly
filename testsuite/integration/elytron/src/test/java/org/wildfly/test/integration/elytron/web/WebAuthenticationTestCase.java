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
 * Test case to test authentication to web applications, initially programatic authentication.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({ WebAuthenticationTestCaseBase.ElytronDomainSetupOverride.class, ServletElytronDomainSetup.class })
public class WebAuthenticationTestCase extends WebAuthenticationTestCaseBase {

    @Override
    protected String getWebXmlName() {
        return "web.xml";
    }

}
