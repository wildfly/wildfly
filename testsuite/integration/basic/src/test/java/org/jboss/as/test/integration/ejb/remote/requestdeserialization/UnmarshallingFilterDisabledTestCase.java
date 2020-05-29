/*
 * Copyright (c) 2020. Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.ejb.remote.requestdeserialization;

import java.io.Serializable;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(UnmarshallingFilterDisabledTestCase.ServerSetup.class)
public class UnmarshallingFilterDisabledTestCase extends AbstactUnmarshallingFilterTestCase {

    // Disables unmarshalling blacklisting.
    public static class ServerSetup implements ServerSetupTask {
        private static final PathAddress PROP_ADDRESS = PathAddress.pathAddress("system-property", "jboss.ejb.unmarshalling.filter.disabled");

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode op = Util.createAddOperation(PROP_ADDRESS);
            op.get("value").set("true");
            ModelNode response = managementClient.getControllerClient().execute(op);
            Assert.assertEquals(response.toString(), "success", response.get("outcome").asString());

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            ModelNode op = Util.createRemoveOperation(PROP_ADDRESS);
            ModelNode response = managementClient.getControllerClient().execute(op);
            Assert.assertEquals(response.toString(), "success", response.get("outcome").asString());
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    @Deployment
    public static Archive<?> createDeployment() {
        return createDeployment(UnmarshallingFilterDisabledTestCase.class);
    }

    @Test
    public void testRemoting() throws NamingException {
        testBlacklistDisabled(false);
    }

    @Test
    public void testHttp() throws NamingException {
        testBlacklistDisabled(true);
    }

    private void testBlacklistDisabled(boolean http) throws NamingException {
        HelloRemote bean = lookup(HelloBean.class.getSimpleName(), HelloRemote.class, http);
        Serializable blacklisted = getTemplatesImpl();
        Response response = bean.sayHello(new Request(blacklisted));
        Assert.assertTrue(response.getGreeting(), response.getGreeting().contains("TemplatesImpl"));
    }
}
