/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.deployment;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.rar.MultipleAdminObject1;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Test the situation when connection factory or Jakarta Messaging destination is configured for resource adapter
 *
 * @author <a href="parsharma@redhat.com">Parul Sharma</a>
 */

@RunWith(Arquillian.class)
@RunAsClient
public class JndiViewOperationJMSTestCase extends JcaMgmtBase {

    static final String rarDeploymentName = "eis.rar";

    @Deployment(name = "rar", order = 1)
    public static Archive<?> deploytRar() {
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, rarDeploymentName);

        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        ja.addPackage(MultipleAdminObject1.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(JndiViewOperationJMSTestCase.class.getPackage(), "ra.xml", "ra.xml");
        return raa;
    }

    @Test
    public void testValueOfJms() throws Exception {
        ModelNode address = null;
        try {
            address = setup();
            ModelNode addr = getAddress();
            ModelNode operation = getOperation(addr);
            ModelNode result = executeOperation(operation);
            ModelNode value = result.get("java: contexts", "java:jboss", "Name3", "class-name");
            Assert.assertEquals("org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl", value.asString());
        } finally {
            if (address != null) {
                remove(address);
            }
        }
    }

    private ModelNode getAddress() {
        return new ModelNode().add(SUBSYSTEM, "naming");
    }

    private ModelNode getOperation(ModelNode address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("jndi-view");
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    private ModelNode setup() throws Exception {

        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "resource-adapters");
        address.add("resource-adapter", rarDeploymentName);
        address.protect();
        ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);
        operation.get("archive").set(rarDeploymentName);
        executeOperation(operation);

        ModelNode addr = address.clone();
        addr.add("admin-objects", "ij_Pool3");

        operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(addr);
        operation.get("jndi-name").set("java:jboss/Name3");
        operation
                .get("class-name")
                .set("org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl");
        executeOperation(operation);

        operation = new ModelNode();
        operation.get(OP).set("activate");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);

        return address;

    }
}
