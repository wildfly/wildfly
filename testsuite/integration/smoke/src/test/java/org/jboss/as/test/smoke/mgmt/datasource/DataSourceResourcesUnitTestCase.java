/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.mgmt.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.management.jca.DsMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Datasource resources unit test.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class DataSourceResourcesUnitTestCase extends DsMgmtTestBase {

    @Test
    public void testReadChildrenResources() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-children-resources");
        operation.get("child-type").set("data-source");
        operation.get(OP_ADDR).set(address);

        final ModelNode result = executeOperation(operation);
        final Map<String, ModelNode> children = getChildren(result);
        Assertions.assertFalse(children.isEmpty());
        for (final Entry<String, ModelNode> child : children.entrySet()) {
            Assertions.assertNotNull(child.getKey(), "Default datasource not found");
            Assertions.assertTrue(child.getValue().hasDefined("connection-url"), "Default datasource have no connection URL");
            Assertions.assertTrue(child.getValue().hasDefined("jndi-name"), "Default datasource have no JNDI name");
            Assertions.assertTrue(child.getValue().hasDefined("driver-name"), "Default datasource have no driver");
        }
    }

    @Test
    public void testReadResourceResources() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-resource-description");

        operation.get(OP_ADDR).set(address);

        final ModelNode result = executeOperation(operation);
        final Map<String, ModelNode> children = getChildren(
                result.get("attributes").get("installed-drivers").get("value-type"));
        Assertions.assertFalse(children.isEmpty());

        HashSet<String> keys = new HashSet<String>();
        for (final Entry<String, ModelNode> child : children.entrySet()) {
            Assertions.assertNotNull(child.getKey(), "Default driver description have no attributes");
            keys.add(child.getKey());
        }
        Assertions.assertTrue(keys.contains("driver-xa-datasource-class-name"), "Default driver description have no xa-datasource-class attribute");
        Assertions.assertTrue(keys.contains("module-slot"), "Default driver description have no module-slot attribute");
        Assertions.assertTrue(keys.contains("driver-name"), "Default driver description have no driver-name attribute");
    }
}
