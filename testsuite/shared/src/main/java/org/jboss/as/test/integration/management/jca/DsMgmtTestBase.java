/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.management.jca;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;

/**
 * Extension of AbstractMgmtTestBase for data source testing.
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class DsMgmtTestBase extends ContainerResourceMgmtTestBase {
    public static ModelNode baseAddress;

    public static void setBaseAddress(String dsType, String dsName) {
        baseAddress = new ModelNode();
        baseAddress.add("subsystem", "datasources");
        baseAddress.add(dsType, dsName);
        baseAddress.protect();
    }

    //@After - called after each test
    protected void removeDs() throws Exception {
        final ModelNode removeOperation = Operations.createRemoveOperation(baseAddress);
        removeOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
        executeOperation(removeOperation);
    }


    protected ModelNode readAttribute(ModelNode address, String attribute) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-attribute");
        operation.get("name").set(attribute);
        operation.get(OP_ADDR).set(address);
        return executeOperation(operation);
    }

    private void testCon(final String dsName, String type) throws Exception {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add(type, dsName);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("test-connection-in-pool");
        operation.get(OP_ADDR).set(address);

        executeOperation(operation);
    }

    protected void testConnection(final String dsName) throws Exception {
        testCon(dsName, "data-source");
    }

    protected void testConnectionXA(final String dsName) throws Exception {
        testCon(dsName, "xa-data-source");
    }

}
