/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the situation when abstract DataSource class is specified when creating a data source.
 *
 * @author lgao
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DatasourceWrongDsClassTestCase extends JcaMgmtBase {

    private static final String DEPLOYMENT = "dummydriver";

    @Deployment(name = DEPLOYMENT)
    public static JavaArchive jdbcArchive() throws Exception {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar");
        ja.addClasses(DummyDataSource.class, DummyXADataSource.class, TestDriver.class);
        ja.addAsServiceProviderAndClasses(Driver.class, TestDriver.class);
        return ja;
    }

    @Test
    public void testWrongDSClass() throws Exception {
        String driverName = DEPLOYMENT + ".jar";
        ModelNode address = getDataSourceAddress("wrongClsDs");
        ModelNode operation = getDataSourceOperation(address, "java:/wrongClsDs", driverName, DummyDataSource.class.getName());
        try {
            executeOperation(operation);
            Assert.fail("Not supposed to succeed");
        } catch (MgmtOperationException e) {
            ModelNode result = e.getResult();
            Assert.assertEquals("failed", result.get("outcome").asString());
            String failDesc = result.get("failure-description").asString();
            Assert.assertTrue(failDesc.contains("WFLYJCA0117"));
            return;
        }
        Assert.fail("Not supposed to be here");
    }

    @Test
    public void testWrongXADSClass() throws Exception {
        String driverName = DEPLOYMENT + ".jar";
        ModelNode address = getXADataSourceAddress("wrongXAClsDs");
        ModelNode operation = getXADataSourceOperation(address, "java:/wrongXAClsDs", driverName, DummyXADataSource.class.getName());
        try {
            executeOperation(operation);
            Assert.fail("Not supposed to succeed");
        } catch (MgmtOperationException e) {
            ModelNode result = e.getResult();
            Assert.assertEquals("failed", result.get("outcome").asString());
            return;
        }
        Assert.fail("Not supposed to be here");
    }

    private ModelNode getXADataSourceAddress(String xaDsName) {
        ModelNode address = new ModelNode()
                .add(SUBSYSTEM, "datasources")
                .add("xa-data-source", xaDsName);
        return address;
    }

    private ModelNode getDataSourceAddress(String dsName) {
        ModelNode address = new ModelNode()
                .add(SUBSYSTEM, "datasources")
                .add("data-source", dsName);
        return address;
    }

    private ModelNode getDataSourceOperation(ModelNode address, String jndiName, String driverName, String dsClsName) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        operation.get("jndi-name").set(jndiName);
        operation.get("driver-name").set(driverName);
        operation.get("datasource-class").set(dsClsName);
        return operation;
    }

    private ModelNode getXADataSourceOperation(ModelNode address, String jndiName, String driverName, String xaDsClsName) {
        ModelNode addOp = new ModelNode();
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(address);
        addOp.get("jndi-name").set(jndiName);
        addOp.get("driver-name").set(driverName);
        addOp.get("xa-datasource-class").set(xaDsClsName);

        ModelNode connProps = new ModelNode();
        connProps.get(OP).set(ADD);
        ModelNode connPropAdd = address.add("connection-properties", "url");
        connProps.get(OP_ADDR).set(connPropAdd);
        connProps.get("value").set("dummy");
        List<ModelNode> operationList = new ArrayList<>(Arrays.asList(addOp, connProps));
        return ModelUtil.createCompositeNode(operationList.toArray(new ModelNode[1]));
    }
}
