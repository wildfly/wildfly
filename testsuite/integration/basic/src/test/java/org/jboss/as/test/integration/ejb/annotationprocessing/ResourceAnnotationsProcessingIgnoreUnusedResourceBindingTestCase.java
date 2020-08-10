/*
 * Copyright 2020 Red Hat, Inc.
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

package org.jboss.as.test.integration.ejb.annotationprocessing;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(ResourceAnnotationsProcessingIgnoreUnusedResourceBindingTestCase.ServerSetup.class)
public class ResourceAnnotationsProcessingIgnoreUnusedResourceBindingTestCase {

    private static final String CLASSES_REFERENCING_INTERFACE_WITHOUT_IMPL = "classes-referencing-interface-without-impl";

    private static final PathAddress PROP_ADDRESS = PathAddress.pathAddress("system-property", "jboss.ee.ignore-unused-resource-binding");
    @ArquillianResource
    private Deployer deployer;

    public static class ServerSetup implements ServerSetupTask {
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

    @Deployment(name = CLASSES_REFERENCING_INTERFACE_WITHOUT_IMPL, managed = false)
    public static WebArchive createDeployment6() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, CLASSES_REFERENCING_INTERFACE_WITHOUT_IMPL + ".war");
        war.addClass(ReferencedBeanInterface.class);
        war.addClass(AbstractReferencingBeanA.class);
        war.addClass(ReferencingClassA.class);
        war.addClass(ReferencingBeanBInterface.class);
        war.addClass(ReferencingClassB.class);
        return war;
    }

    @Test
    public void testClassesReferencingInterfaceWithoutImplIgnoreUnusedResourceBinding() {
        // https://issues.redhat.com/browse/WFLY-13719 Test effect of system property "jboss.ee.ignore-unused-resource-binding"
        // which allow to ignore unused resource binding.
        tryDeployment(CLASSES_REFERENCING_INTERFACE_WITHOUT_IMPL);
    }

    private void tryDeployment(String name) {
        deployer.deploy(name);
        deployer.undeploy(name);
    }
}
