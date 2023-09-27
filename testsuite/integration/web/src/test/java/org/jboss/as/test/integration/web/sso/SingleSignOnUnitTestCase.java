/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests of web app single sign-on
 *
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SingleSignOnUnitTestCase.SingleSignOnUnitTestCaseSetup.class)
@Ignore(value = "ARQ-791 Arquillian is unable to reconnect to JMX server if the connection is lost")
@Category(CommonCriteria.class)
public class SingleSignOnUnitTestCase {

    private static Logger log = Logger.getLogger(SingleSignOnUnitTestCase.class);

    static class SingleSignOnUnitTestCaseSetup implements ServerSetupTask {

        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            SSOTestBase.addSso(managementClient.getControllerClient());
            SSOTestBase.restartServer(managementClient.getControllerClient());
        }

        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            SSOTestBase.removeSso(managementClient.getControllerClient());
        }
    }

    /**
     * One time setup for all SingleSignOnUnitTestCase unit tests
     */
    @Deployment(name = "web-sso.ear", testable = false)
    public static EnterpriseArchive deployment() {
        return SSOTestBase.createSsoEar();
    }

    /**
     * Test single sign-on across two web apps using form based auth
     */
    @Test
    public void testFormAuthSingleSignOn(@ArquillianResource URL baseURLNoAuth) throws Exception {
        log.trace("+++ testFormAuthSingleSignOn");
        SSOTestBase.executeFormAuthSingleSignOnTest(baseURLNoAuth, baseURLNoAuth, log);
    }

    /**
     * Test single sign-on across two web apps using form based auth
     */
    @Test
    public void testNoAuthSingleSignOn(@ArquillianResource URL baseURLNoAuth) throws Exception {
        log.trace("+++ testNoAuthSingleSignOn");
        SSOTestBase.executeNoAuthSingleSignOnTest(baseURLNoAuth, baseURLNoAuth, log);
    }

}
