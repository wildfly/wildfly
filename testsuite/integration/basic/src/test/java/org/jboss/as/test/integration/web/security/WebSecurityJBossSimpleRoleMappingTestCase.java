/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

/**
 * Unit Test web security
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebSecurityJBossSimpleRoleMappingTestCase extends WebSecurityFORMTestCase {

    public static final String deploymentName = "web-secure.war";

    @ArquillianResource @OperateOnDeployment(deploymentName)
    URL deploymentUrl;

    @Before
    public void init() {
        // make URL params flexible in case we are running tests against something different than localhost:8080
        setHostname(deploymentUrl.getHost());
        setPort(deploymentUrl.getPort());
    }


    @Deployment(name = deploymentName, order = 1, testable = false)
    public static WebArchive deployment() {
        // FIXME hack to get things prepared before the deployment happens
        try {
            // create required security domains
            SecurityTest.createSimpleRoleMappingSecurityDomain();
            //Thread.sleep(1000 * 60 * 10);
        } catch (Exception e) {
            // ignore
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL webxml = tccl.getResource("web-secure.war/web.xml");
        WebArchive war = WebSecurityPasswordBasedBase.create("web-secure.war", SecuredServlet.class, true, webxml);

        war.addAsWebResource(tccl.getResource("web-secure.war/login.jsp"), "login.jsp");
        war.addAsWebResource(tccl.getResource("web-secure.war/error.jsp"), "error.jsp");
        war.addAsWebInfResource(tccl.getResource("web-secure.war/jboss-web.xml"), "jboss-web.xml");

        WebSecurityPasswordBasedBase.printWar(war);
        return war;
    }


    /**
     * Negative test as marcus doesn't have proper role in module mapping created.
     */
    @Override
    @Test
    public void testPasswordBasedUnsuccessfulAuth() throws Exception {
        makeCall("marcus", "marcus", 403);
    }

    /**
     * At this time peter can go through because he has role mapped in the map-module option.
     * @throws Exception
     */
    @Test
    public void testPrincipalMappingOnRole() throws Exception {
        makeCall("peter", "peter", 200);
    }


}