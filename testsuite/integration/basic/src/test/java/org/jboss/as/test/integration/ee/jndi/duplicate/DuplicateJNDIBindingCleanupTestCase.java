/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.jndi.duplicate;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.management.JndiViewOperation;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Deploy apps with same context to check if JNDI bindings are properly cleared on undeploy/failure.
 *
 * @author baranowb
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DuplicateJNDIBindingCleanupTestCase {
    private static final String APP1_ARCHIVE = "app-1";
    private static final String APP2_ARCHIVE = "app-2";
    private static final boolean IS_OS_CENTOS = "centos".equalsIgnoreCase(WildFlySecurityManager.getEnvPropertyPrivileged("os.name", ""));

    @ArquillianResource
    private Deployer deployer;
    @ContainerResource
    ManagementClient managementClient;

    public static Archive<?> createDeployment(final String name) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.addClasses(StatelessEJB.class, StatelessRemote.class);
        war.addAsWebInfResource(DuplicateJNDIBindingCleanupTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        return war;
    }

    @Deployment(name = APP1_ARCHIVE, managed = false)
    public static Archive<?> createDeployment1() {
        return createDeployment(APP1_ARCHIVE);
    }

    @Deployment(name = APP2_ARCHIVE, managed = false)
    public static Archive<?> createDeployment2() {
        return createDeployment(APP2_ARCHIVE);
    }

    @BeforeClass
    public static void beforeClass() throws IOException, InterruptedException {
        Assume.assumeFalse(IS_OS_CENTOS);
    }

    @Test
    public void testDeploy() throws Exception {
        deployer.deploy(APP1_ARCHIVE);
        try {
            deployer.deploy(APP2_ARCHIVE);
        } catch (Exception de) {
        }
        deployer.undeploy(APP1_ARCHIVE);
        deployer.undeploy(APP2_ARCHIVE);
        final ModelNode op = new ModelNode();
        op.get(OP).set(JndiViewOperation.OPERATION_NAME);
        final ModelNode address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "naming")).toModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(address);
        final ModelNode result = managementClient.getControllerClient().execute(op);
        // this should always be false, but lets test
        final ModelNode exported = result.get("result").get("java: contexts").get("java:jboss/exported");
        Assert.assertFalse(
                "Failed to remove app exported context: "
                        + exported.keys(),
                        exported.keys().contains(APP1_ARCHIVE));
        Assert.assertFalse(
                "Failed to remove app exported context: "
                        + exported.keys(),
                        exported.keys().contains(APP2_ARCHIVE));
    }
}
