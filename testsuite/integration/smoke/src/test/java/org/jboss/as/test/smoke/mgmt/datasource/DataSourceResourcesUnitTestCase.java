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

package org.jboss.as.test.smoke.mgmt.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.jca.DsMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Datasource resources unit test.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
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
        Assert.assertFalse(children.isEmpty());
        for (final Entry<String, ModelNode> child : children.entrySet()) {
            Assert.assertNotNull("Default datasource not found", child.getKey());
            Assert.assertTrue("Default datasource have no connection URL", child.getValue().hasDefined("connection-url"));
            Assert.assertTrue("Default datasource have no JNDI name", child.getValue().hasDefined("jndi-name"));
            Assert.assertTrue("Default datasource have no driver", child.getValue().hasDefined("driver-name"));
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
        Assert.assertFalse(children.isEmpty());

        HashSet<String> keys = new HashSet<String>();
        for (final Entry<String, ModelNode> child : children.entrySet()) {
            Assert.assertNotNull("Default driver description have no attributes", child.getKey());
            keys.add(child.getKey());
        }
        Assert.assertTrue("Default driver description have no xa-datasource-class attribute", keys.contains("driver-xa-datasource-class-name"));
        Assert.assertTrue("Default driver description have no module-slot attribute", keys.contains("module-slot"));
        Assert.assertTrue("Default driver description have no driver-name attribute", keys.contains("driver-name"));
    }
}
