/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.excludesubsystem;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests excluding subsystems via jboss-deployment-structure.xml
 * Test for WFLY-12472
 */
@RunWith(Arquillian.class)
public class ExcludeEESubsystemTestCase {

    private static final Logger logger = Logger.getLogger(ExcludeEESubsystemTestCase.class);
    private static final String EXCLUDE_SUBSYSTEM_EE = "excludeSubsystemEE";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = EXCLUDE_SUBSYSTEM_EE, managed = false)
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EXCLUDE_SUBSYSTEM_EE + ".war");
        jar.addAsManifestResource(ExcludeEESubsystemTestCase.class.getPackage(), "jboss-deployment-structure-exclude-ee.xml",
                "jboss-deployment-structure.xml");
        jar.addPackage(ExcludeEESubsystemTestCase.class.getPackage());
        return jar;
    }

    // Test that simple deployment runs without ee subsystem as required by Infinispan
    @Test
    public void testDeploy() throws Exception {
        deployer.deploy(EXCLUDE_SUBSYSTEM_EE);
        deployer.undeploy(EXCLUDE_SUBSYSTEM_EE);
    }

}
