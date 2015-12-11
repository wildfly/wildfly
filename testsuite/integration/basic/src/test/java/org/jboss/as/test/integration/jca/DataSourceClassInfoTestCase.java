/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jca;

import static org.jboss.as.controller.client.helpers.ClientConstants.INCLUDE_RUNTIME;

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
 *
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
        ModelNode dsInfoList = result.get("result").get("data-source-class-info");
        Assert.assertNotNull(dsInfoList);
        ModelNode dsInfo = dsInfoList.get(0).get("org.h2.jdbcx.JdbcDataSource");
        Assert.assertNotNull(dsInfo);

        Assert.assertEquals("java.lang.String", dsInfo.get("Description").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("User").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("URL").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("Password").asString());
        Assert.assertEquals("int", dsInfo.get("LoginTimeout").asString());

        Assert.assertEquals("undefined", dsInfo.get("LogWriter").asString());
    }

}