/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.structure.parsing;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * Tests that parsing of a jboss-deployment-structure.xml using 1.1 version xsd, works as expected
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class JBossDeploymentStructure11ParsingTestCase {

    private static final Logger logger = Logger.getLogger(JBossDeploymentStructure11ParsingTestCase.class);

    private static final String MODULE_NAME = "jboss-deployment-structure-11-parsing-test";

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(JBossDeploymentStructure11ParsingTestCase.class.getPackage());
        ejbJar.addAsManifestResource(JBossDeploymentStructure11ParsingTestCase.class.getPackage(), "jboss-deployment-structure_1_1.xml", "jboss-deployment-structure.xml");
        return ejbJar;
    }

    /**
     * Tests that the deployment containing the jboss-deployment-structure.xml was parsed and deployed successfully.
     */
    @Test
    public void testSuccessfulDeployment() throws Exception {
        // just a lookup and invocation on the EJB should be fine, since it indicates that the deployment
        // was deployed successfully.
        final NoOpEJB noOpEJB = InitialContext.doLookup("java:module/" + NoOpEJB.class.getSimpleName() + "!" + NoOpEJB.class.getName());
        noOpEJB.doNothing();
    }
}
