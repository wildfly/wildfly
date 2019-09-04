/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
