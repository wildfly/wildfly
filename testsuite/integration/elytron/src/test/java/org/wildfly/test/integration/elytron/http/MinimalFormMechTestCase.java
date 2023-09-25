/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.http;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.integration.web.sso.LogoutServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;

/**
 * Test of FORM HTTP mechanism using a direct reference to the security domain instead of an authentication factory.
 *
 * @author Jan Kalina
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ MinimalFormMechTestCase.ServerSetup.class })
public class MinimalFormMechTestCase extends FormMechTestBase {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
            .addClasses(SimpleServlet.class)
            .addClasses(LogoutServlet.class)
            .addAsWebInfResource(Utils.getJBossWebXmlAsset(APP_DOMAIN), "jboss-web.xml")
            .addAsWebResource(new StringAsset(LOGIN_PAGE_CONTENT), "login.html")
            .addAsWebResource(new StringAsset(ERROR_PAGE_CONTENT), "error.html")
            .addAsWebInfResource(FormMechTestCase.class.getPackage(), NAME + "-web.xml", "web.xml");
    }

    static class ServerSetup extends AbstractMechTestBase.ServerSetup {

        @Override
        protected boolean useAuthenticationFactory() {
            return false;
        }

        @Override
        protected MechanismConfiguration getMechanismConfiguration() {
            // As we are not using an authentication factory the mechanisms do not require configuration.
            return null;
        }

    }
}
