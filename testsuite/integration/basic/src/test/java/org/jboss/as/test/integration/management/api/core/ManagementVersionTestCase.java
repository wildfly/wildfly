/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.core;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.io.IOException;
import java.net.URL;

import javax.enterprise.inject.Model;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for functionality added with AS7-2234.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ManagementVersionTestCase extends AbstractMgmtTestBase {


    @ArquillianResource
    URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(ManagementVersionTestCase.class);
        return ja;
    }

    @Before
    public void before() throws IOException {
        initModelControllerClient(url.getHost(), MGMT_PORT);
    }

    @AfterClass
    public static void after() throws IOException {
        closeModelControllerClient();
    }

    @Test
    public void testRootResource() throws Exception  {
        ModelNode op = createOpNode(null, "read-resource");

        ModelNode result = executeOperation(op);
        ModelNode major = result.get("management-major-version");
        ModelNode minor = result.get("management-minor-version");
        Assert.assertEquals(ModelType.INT, major.getType());
        Assert.assertEquals(ModelType.INT, minor.getType());
        Assert.assertEquals(Version.MANAGEMENT_MAJOR_VERSION, major.asInt());
        Assert.assertEquals(Version.MANAGEMENT_MINOR_VERSION, minor.asInt());
    }

    @Test
    public void testExtensions() throws Exception {
        ModelNode op = createOpNode(null, "read-children-resources");
        op.get("child-type").set("extension");
        op.get("recursive").set(true);
        op.get("include-runtime").set(true);

        ModelNode result = executeOperation(op);
        for (Property extension : result.asPropertyList()) {
            String extensionName = extension.getName();
            ModelNode subsystems = extension.getValue().get("subsystem");
            Assert.assertEquals(extensionName + " has no subsystems", ModelType.OBJECT, subsystems.getType());
            for (Property subsystem : subsystems.asPropertyList()) {
                String subsystemName = subsystem.getName();
                ModelNode value = subsystem.getValue();
                Assert.assertEquals(subsystemName + " has major version", ModelType.INT, value.get("management-major-version").getType());
                Assert.assertEquals(subsystemName + " has minor version", ModelType.INT, value.get("management-minor-version").getType());
                Assert.assertEquals(subsystemName + " has namespaces", ModelType.LIST, value.get("xml-namespaces").getType());
                Assert.assertTrue(subsystemName + " has positive major version", value.get("management-major-version").asInt() > 0);
                Assert.assertTrue(subsystemName + " has positive minor version", value.get("management-minor-version").asInt() >= 0);
                Assert.assertTrue(subsystemName + " has more than zero namespaces", value.get("xml-namespaces").asInt() > 0);
            }
        }
    }
}
