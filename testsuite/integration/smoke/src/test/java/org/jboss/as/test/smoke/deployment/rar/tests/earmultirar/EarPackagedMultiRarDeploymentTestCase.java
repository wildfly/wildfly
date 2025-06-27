/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.tests.earmultirar;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.smoke.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.deployment.rar.MultipleAdminObject2;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory1;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory2;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * @author <a href="robert.reimann@googlemail.com">Robert Reimann</a>
 *         Deployment of a RAR packaged inside an EAR.
 */
@ExtendWith(ArquillianExtension.class)
public class EarPackagedMultiRarDeploymentTestCase {

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
                addClasses(EarPackagedMultiRarDeploymentTestCase.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        ear.addAsLibrary(ja);
        ear.addAsModule(raa);
        ear.addAsModule(raa2);
        ear.addAsManifestResource(EarPackagedMultiRarDeploymentTestCase.class.getPackage(), "application.xml", "application.xml");
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
        assertNotNull(connectionFactory1, "CF1 not found");
        assertNotNull(adminObject1, "AO1 not found");

        assertNotNull(connectionFactory2, "CF2 not found");
        assertNotNull(adminObject2, "AO2 not found");
    }
}
