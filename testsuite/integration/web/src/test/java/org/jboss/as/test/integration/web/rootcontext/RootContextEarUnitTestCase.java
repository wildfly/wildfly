/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.rootcontext;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

/**
 * This class tests a root context deployed as an EAR or a WAR.
 *
 * @author Stan.Silvert@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RootContextEarUnitTestCase.RootContextEarUnitTestCaseSetup.class)
public class RootContextEarUnitTestCase {

    static class RootContextEarUnitTestCaseSetup extends AbstractMgmtServerSetupTask {

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {
            RootContextUtil.createVirutalHost(managementClient.getControllerClient(), HOST);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            RootContextUtil.removeVirtualHost(managementClient.getControllerClient(), HOST);
        }
    }


    private static String HOST = "context-host";

    @Deployment(name = "root-web.ear")
    public static EnterpriseArchive earDeployment() {

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/rootcontext/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "root-web.war");

        war.setWebXML(tccl.getResource(resourcesLocation + "root-web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "root-web.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "application-root.xml"));
        ear.addAsModule(war);

        return ear;
    }

    @Test
    public void testRootContextEAR(@ArquillianResource URL url) throws Exception {
        String response = RootContextUtil.hitRootContext(url, HOST);
        assertTrue(response.contains("A Root Context Page"));
    }

}
