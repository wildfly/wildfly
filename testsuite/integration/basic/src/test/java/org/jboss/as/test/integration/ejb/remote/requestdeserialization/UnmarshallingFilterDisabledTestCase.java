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
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(UnmarshallingFilterDisabledTestCase.ServerSetup.class)
public class UnmarshallingFilterDisabledTestCase extends AbstactUnmarshallingFilterTestCase {

    // Disables unmarshalling blocklisting.
    public static class ServerSetup implements ServerSetupTask {
        private static final PathAddress DESERIALTEMPLATE_ADDRESS =  PathAddress.pathAddress("system-property", "jdk.xml.enableTemplatesImplDeserialization");
        private static final PathAddress PROP_ADDRESS = PathAddress.pathAddress("system-property", "jboss.ejb.unmarshalling.filter.disabled");

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            if (AssumeTestGroupUtil.isSecurityManagerEnabled()) {
                //When SM is enabled, deserializing TemplatesImpl in JDK is disabled and
                //set the jdk.xml.enableTemplatesImplDeserialization system property
                //to enable it
                ModelNode templateOp = Util.createAddOperation(DESERIALTEMPLATE_ADDRESS);
                templateOp.get("value").set("true");
                CoreUtils.applyUpdate(templateOp, managementClient.getControllerClient());
            }

            ModelNode op = Util.createAddOperation(PROP_ADDRESS);
            op.get("value").set("true");
            CoreUtils.applyUpdate(op, managementClient.getControllerClient());

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            if (AssumeTestGroupUtil.isSecurityManagerEnabled()) {
                CoreUtils.applyUpdate(Util.createRemoveOperation(DESERIALTEMPLATE_ADDRESS),
                        managementClient.getControllerClient());
            }
            ModelNode removeOp = Util.createRemoveOperation(PROP_ADDRESS);
            CoreUtils.applyUpdate(removeOp, managementClient.getControllerClient());
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    @Deployment
    public static Archive<?> createDeployment() {
        return createDeployment(UnmarshallingFilterDisabledTestCase.class);
    }

    @Test
    public void testRemoting() throws NamingException {
        testBlocklistDisabled(false);
    }

    @Test
    public void testHttp() throws NamingException {
        testBlocklistDisabled(true);
    }

    private void testBlocklistDisabled(boolean http) throws NamingException {
        HelloRemote bean = lookup(HelloBean.class.getSimpleName(), HelloRemote.class, http);
        Serializable blocklisted = getTemplatesImpl();
        Response response = bean.sayHello(new Request(blocklisted));
        Assert.assertTrue(response.getGreeting(), response.getGreeting().contains("TemplatesImpl"));
    }
}
