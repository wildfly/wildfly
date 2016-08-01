/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.smoke.deployment.rar.tests.earmultirar;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.smoke.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.deployment.rar.MultipleAdminObject2;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory1;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory2;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author <a href="robert.reimann@googlemail.com">Robert Reimann</a>
 *         Deployment of a RAR packaged inside an EAR.
 */
@RunWith(Arquillian.class)
public class EarPackagedMultiRarDeploymentTestCase extends ContainerResourceMgmtTestBase {

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static EnterpriseArchive createDeployment() throws Exception {

        String deploymentName = "ear_packaged.ear";
        String subDeploymentName = "ear_packaged.rar";
        String subDeploymentName2 = "ear_packaged2.rar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, subDeploymentName);


        raa.addAsManifestResource(EarPackagedMultiRarDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(EarPackagedMultiRarDeploymentTestCase.class.getPackage(), "ironjacamar.xml", "ironjacamar.xml");

        ResourceAdapterArchive raa2 =
                ShrinkWrap.create(ResourceAdapterArchive.class, subDeploymentName2);

        raa2.addAsManifestResource(EarPackagedMultiRarDeploymentTestCase.class.getPackage(), "ra2.xml", "ra.xml")
                .addAsManifestResource(EarPackagedMultiRarDeploymentTestCase.class.getPackage(), "ironjacamar2.xml", "ironjacamar.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).
                addClasses(EarPackagedMultiRarDeploymentTestCase.class, MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        ear.addAsLibrary(ja);
        ear.addAsModule(raa);
        ear.addAsModule(raa2);
        ear.addAsManifestResource(EarPackagedMultiRarDeploymentTestCase.class.getPackage(), "application.xml", "application.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"), "MANIFEST.MF");
        return ear;
    }

    @Resource(mappedName = "java:jboss/name3")
    private MultipleConnectionFactory1 connectionFactory1;


    @Resource(mappedName = "java:jboss/Name5")
    private MultipleAdminObject1 adminObject1;

    @Resource(mappedName = "java:jboss/name2")
    private MultipleConnectionFactory2 connectionFactory2;


    @Resource(mappedName = "java:jboss/Name4")
    private MultipleAdminObject2 adminObject2;


    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
        assertNotNull("CF1 not found", connectionFactory1);
        assertNotNull("AO1 not found", adminObject1);

        assertNotNull("CF2 not found", connectionFactory2);
        assertNotNull("AO2 not found", adminObject2);
    }
}
