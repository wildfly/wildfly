/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
