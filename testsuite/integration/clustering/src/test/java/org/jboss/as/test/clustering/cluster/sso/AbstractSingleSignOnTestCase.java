/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.sso;

import java.net.URL;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.integration.web.sso.LogoutServlet;
import org.jboss.as.test.integration.web.sso.SSOTestBase;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * @author <a href="mailto:dpospisi@redhat.com">Dominik Pospisil</a>
 */
public abstract class AbstractSingleSignOnTestCase extends AbstractClusteringTestCase {

    private static Logger log = Logger.getLogger(AbstractSingleSignOnTestCase.class);

    /**
     * Test single sign-on across two web apps using form based auth
     */
    @Test
    public void testFormAuthSingleSignOn(
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        log.trace("+++ testFormAuthSingleSignOn");
        SSOTestBase.executeFormAuthSingleSignOnTest(new URL(baseURL1, "/"), new URL(baseURL2, "/"), log);
    }

    /**
     * Test single sign-on across two web apps using form based auth
     */
    @Test
    public void testNoAuthSingleSignOn(
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        log.trace("+++ testNoAuthSingleSignOn");
        SSOTestBase.executeNoAuthSingleSignOnTest(new URL(baseURL1, "/"), new URL(baseURL2, "/"), log);
    }

    /**
     * Test single sign-on is destroyed after session timeout
     *
     * Testing https://issues.jboss.org/browse/WFLY-5422
     */
    @Test
    public void testSessionTimeoutDestroysSSO(
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        log.trace("+++ testSessionTimeoutDestroysSSO");
        SSOTestBase.executeFormAuthSSOTimeoutTest(new URL(baseURL1, "/"), new URL(baseURL2, "/"), log);
    }
}
