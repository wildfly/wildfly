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
