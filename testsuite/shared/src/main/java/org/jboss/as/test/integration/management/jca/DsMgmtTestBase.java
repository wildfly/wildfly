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

package org.jboss.as.test.integration.management.jca;

import java.util.List;

import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension.DataSourceSubsystemParser;
import org.jboss.as.connector.subsystems.datasources.Namespace;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

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

    protected List<ModelNode> marshalAndReparseDsResources(String childType) throws Exception {
        DataSourceSubsystemParser parser = new DataSourceSubsystemParser();
        return xmlToModelOperations(modelToXml("datasources", childType, parser), Namespace.CURRENT.getUriString(), parser);
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
