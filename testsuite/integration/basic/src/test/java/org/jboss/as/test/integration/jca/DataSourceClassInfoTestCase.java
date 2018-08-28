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

package org.jboss.as.test.integration.jca;

import static org.jboss.as.controller.client.helpers.ClientConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_ATTRIBUTE_OPERATION;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourceClassInfoTestCase extends ContainerResourceMgmtTestBase {

    private ModelNode getDsClsInfoOperation(String driverName) {
        ModelNode driverAddress = new ModelNode();
        driverAddress.add("subsystem", "datasources");
        driverAddress.add("jdbc-driver", driverName);
        ModelNode op = Operations.createReadResourceOperation(driverAddress);
        op.get(INCLUDE_RUNTIME).set(true);
        return op;
    }

    @Test
    public void testGetDsClsInfo() throws Exception {
        ModelNode operation = getDsClsInfoOperation("h2");
        ModelNode result = getManagementClient().getControllerClient().execute(operation);

        Assert.assertNotNull(result);
        Assert.assertEquals("success", result.get("outcome").asString());
        ModelNode dsInfoList = result.get("result").get("datasource-class-info");
        Assert.assertNotNull(dsInfoList);
        ModelNode dsInfo = dsInfoList.get(0).get("org.h2.jdbcx.JdbcDataSource");
        Assert.assertNotNull(dsInfo);

        Assert.assertEquals("java.lang.String", dsInfo.get("description").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("user").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("url").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("password").asString());
        Assert.assertEquals("int", dsInfo.get("loginTimeout").asString());

    }

    @Test
    public void testGetDsClsInfoByReadAttribute() throws Exception {
        ModelNode driverAddress = new ModelNode();
        driverAddress.add("subsystem", "datasources");
        driverAddress.add("jdbc-driver", "h2");
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(driverAddress);
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get("name").set("datasource-class-info");

        ModelNode result = getManagementClient().getControllerClient().execute(op);

        Assert.assertNotNull(result);
        Assert.assertEquals("success", result.get("outcome").asString());
        ModelNode dsInfoList = result.get("result");
        Assert.assertNotNull(dsInfoList);
        ModelNode dsInfo = dsInfoList.get(0).get("org.h2.jdbcx.JdbcDataSource");
        Assert.assertNotNull(dsInfo);

        Assert.assertEquals("java.lang.String", dsInfo.get("description").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("user").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("url").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("password").asString());
        Assert.assertEquals("int", dsInfo.get("loginTimeout").asString());

    }

    @Test
    public void testInstalledDriverList() throws Exception {
        // installed-drivers-list
        ModelNode subsysAddr = new ModelNode();
        subsysAddr.add("subsystem", "datasources");
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(subsysAddr);
        op.get(OP).set("installed-drivers-list");

        ModelNode result = getManagementClient().getControllerClient().execute(op);

        Assert.assertNotNull(result);
        Assert.assertEquals("success", result.get("outcome").asString());
        ModelNode dsInfoList = result.get("result");
        Assert.assertNotNull(dsInfoList);
        Assert.assertTrue(dsInfoList.get(0).has("driver-datasource-class-name"));
        ModelNode dsInfo = dsInfoList.get(0).get("datasource-class-info").get(0).get("org.h2.jdbcx.JdbcDataSource");
        Assert.assertNotNull(dsInfo);

        Assert.assertEquals("java.lang.String", dsInfo.get("description").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("user").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("url").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("password").asString());
        Assert.assertEquals("int", dsInfo.get("loginTimeout").asString());
    }

    @Test
    public void testGetInstalledDriver() throws Exception {
        // get-installed-driver(driver-name=h2)
        ModelNode subsysAddr = new ModelNode();
        subsysAddr.add("subsystem", "datasources");
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(subsysAddr);
        op.get(OP).set("get-installed-driver");
        op.get("driver-name").set("h2");

        ModelNode result = getManagementClient().getControllerClient().execute(op);

        Assert.assertNotNull(result);
        Assert.assertEquals("success", result.get("outcome").asString());
        ModelNode dsInfoList = result.get("result");
        Assert.assertNotNull(dsInfoList);
        Assert.assertTrue(dsInfoList.get(0).has("driver-datasource-class-name"));
        ModelNode dsInfo = dsInfoList.get(0).get("datasource-class-info").get(0).get("org.h2.jdbcx.JdbcDataSource");
        Assert.assertNotNull(dsInfo);

        Assert.assertEquals("java.lang.String", dsInfo.get("description").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("user").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("url").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("password").asString());
        Assert.assertEquals("int", dsInfo.get("loginTimeout").asString());
    }

}