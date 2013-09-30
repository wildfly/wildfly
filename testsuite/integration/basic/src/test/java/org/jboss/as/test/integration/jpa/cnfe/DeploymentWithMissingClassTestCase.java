/*
 * Copyright (C) 2013 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.test.integration.jpa.cnfe;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
public class DeploymentWithMissingClassTestCase {

    private static final String DEPLOYMENT = "cnfe";

    @ArquillianResource
    public Deployer deployer;

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    public static Archive<?> runAsStartupTransitiveDeployment() {
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addClass(Employee.class)
                .addPackage(Assert.class.getPackage())
                .addClass(DeploymentWithMissingClassTestCase.class)
                .addAsManifestResource(DeploymentWithMissingClassTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testDeployingJarWithMissingClass() {
        try {
            deployer.deploy(DEPLOYMENT);
        } catch (Exception dex) {
            Assert.fail("Deployment should succeed " + dex.getMessage());
        } finally {
            deployer.undeploy(DEPLOYMENT);
        }
    }
}
