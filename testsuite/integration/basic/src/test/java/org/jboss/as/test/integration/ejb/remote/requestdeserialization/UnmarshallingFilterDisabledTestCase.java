/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.requestdeserialization;

import java.io.Serializable;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
@ServerSetup(UnmarshallingFilterDisabledTestCase.ServerSetup.class)
public class UnmarshallingFilterDisabledTestCase extends AbstactUnmarshallingFilterTestCase {

    // Disables EJB unmarshalling blocklisting and re-enables JDK deserialization of TemplatesImpl.
    public static class ServerSetup implements ServerSetupTask {
        // WFLY-22374 Deserialization of TemplatesImpl is disabled by default since JDK 17.0.21, 21.0.13 and 25.0.5 (see JDK-8384654)
        private static final PathAddress ENABLE_TEMPLATES_IMPL_DESERIALIZATION_PROPERTY_ADDRESS = PathAddress.pathAddress("system-property", "jdk.xml.enableTemplatesImplDeserialization");
        private static final PathAddress UNMARSHALLING_FILTER_DISABLED_PROPERTY_ADDRESS = PathAddress.pathAddress("system-property", "jboss.ejb.unmarshalling.filter.disabled");

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode templatesImplOp = Util.createAddOperation(ENABLE_TEMPLATES_IMPL_DESERIALIZATION_PROPERTY_ADDRESS);
            templatesImplOp.get("value").set("true");
            CoreUtils.applyUpdate(templatesImplOp, managementClient.getControllerClient());

            ModelNode filterOp = Util.createAddOperation(UNMARSHALLING_FILTER_DISABLED_PROPERTY_ADDRESS);
            filterOp.get("value").set("true");
            CoreUtils.applyUpdate(filterOp, managementClient.getControllerClient());

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            CoreUtils.applyUpdate(Util.createRemoveOperation(ENABLE_TEMPLATES_IMPL_DESERIALIZATION_PROPERTY_ADDRESS), managementClient.getControllerClient());
            CoreUtils.applyUpdate(Util.createRemoveOperation(UNMARSHALLING_FILTER_DISABLED_PROPERTY_ADDRESS), managementClient.getControllerClient());
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
        Assertions.assertTrue(response.getGreeting().contains("TemplatesImpl"), response.getGreeting());
    }
}
