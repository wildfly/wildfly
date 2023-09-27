/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidConnectionFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a> JBQA-5905
 */
@RunWith(Arquillian.class)
public class NegativeValidationTestCase {

    @ArquillianResource
    Deployer deployer;

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    public static ResourceAdapterArchive createDeployment(String ij) throws Exception {
        String deploymentName = (ij != null ? ij : "valid");

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName + ".rar");
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, deploymentName + ".jar");
        ja.addPackage(ValidConnectionFactory.class.getPackage()).addClasses(NegativeValidationTestCase.class);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(NegativeValidationTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(
                        NegativeValidationTestCase.class.getPackage(), "ironjacamar" + (ij != null ? "-" + ij : "") + ".xml", "ironjacamar.xml");

        return raa;
    }

    @Deployment(name = "wrong-ao", managed = false)
    public static ResourceAdapterArchive createAODeployment() throws Exception {
        return createDeployment("wrong-ao");
    }

    @Test(expected = Exception.class)
    public void testWrongAO() {
        deployer.deploy("wrong-ao");
    }

    @Deployment(name = "wrong-cf", managed = false)
    public static ResourceAdapterArchive createCfDeployment() throws Exception {
        return createDeployment("wrong-cf");
    }

    @Test(expected = Exception.class)
    public void testWrongCf() {
        deployer.deploy("wrong-cf");
    }

    @Deployment(name = "wrong-ra", managed = false)
    public static ResourceAdapterArchive createRaDeployment() throws Exception {
        return createDeployment("wrong-ra");
    }

    @Test(expected = Exception.class)
    public void testWrongRA() {
        deployer.deploy("wrong-ra");
    }

}
