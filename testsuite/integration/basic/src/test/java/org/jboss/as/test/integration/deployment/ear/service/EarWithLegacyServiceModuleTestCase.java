/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.deployment.ear.service;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for a ear deployment that contains an unsupported legacy module of type service. The module should be ignored, and the
 * deployment of the ear succeed.
 *
 * @author Eduardo Martins
 *
 */
@RunWith(Arquillian.class)
public class EarWithLegacyServiceModuleTestCase {

    private static final String UNMANAGED_DEPLOYMENT_NAME = "test-ear";

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = EarWithLegacyServiceModuleTestCase.UNMANAGED_DEPLOYMENT_NAME, managed = false)
    public static EnterpriseArchive getEar() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear");
        ear.addAsApplicationResource(EarWithLegacyServiceModuleTestCase.class.getResource("application.xml"), "application.xml");
        ear.addAsApplicationResource(EarWithLegacyServiceModuleTestCase.class.getResource("jboss-app.xml"), "jboss-app.xml");
        ear.addAsModule(EarWithLegacyServiceModuleTestCase.class.getResource("test-service.xml"), "test-service.xml");
        return ear;
    }

    @Test
    public void testEarWithOldServiceXMLDeployment() throws Exception {
        deployer.deploy(EarWithLegacyServiceModuleTestCase.UNMANAGED_DEPLOYMENT_NAME);
        deployer.undeploy(EarWithLegacyServiceModuleTestCase.UNMANAGED_DEPLOYMENT_NAME);
    }

}
