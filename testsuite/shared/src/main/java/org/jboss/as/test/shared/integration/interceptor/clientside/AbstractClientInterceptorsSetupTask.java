/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.shared.integration.interceptor.clientside;

import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;
import static org.jboss.as.controller.client.helpers.ClientConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * An abstract setup task for setting up and removing client-side interceptors.
 * Packs them into $JBOSS_HOME/modules folder, modifies Enterprise Beans 3 subsystem 'client-interceptors' attribute.
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
public abstract class AbstractClientInterceptorsSetupTask {

    public static final String DEPLOYMENT_NAME_SERVER = "server";
    public static final String DEPLOYMENT_NAME_CLIENT = "client";

    public static final String TARGER_CONTAINER_SERVER = "multinode-server";
    public static final String TARGER_CONTAINER_CLIENT = "multinode-client";

    public static class SetupTask implements ServerSetupTask, InterceptorsSetupTask {
        private List<InterceptorModule> interceptorModules;

        public SetupTask() {
            this.interceptorModules = getModules();
        }

        @Override
        public List<InterceptorModule> getModules() {
            // overriden in subclasses
            return new ArrayList<>();
        }

        /**
         * Pack a sample interceptor to module and place to $JBOSS_HOME/modules directory
         */
        @Override
        public void packModule(InterceptorModule module) throws Exception {
            URL url = module.getModuleXmlPath();
            if (url == null) {
                throw new IllegalStateException("Could not find " + module.getModuleXmlName());
            }
            File moduleXmlFile = new File(url.toURI());
            module.setTestModule(new TestModule(module.getModuleName(), moduleXmlFile));
            JavaArchive jar = module.getTestModule().addResource(module.getJarName());
            jar.addClass(module.getInterceptorClass());
            module.getTestModule().create(true);
        }

        /**
         * /subsystem=ejb3:write-attribute(name=client-interceptors,value=[{module=moduleName,class=className}])
         */
        @Override
        public void modifyClientInterceptors(List<InterceptorModule> interceptorModules, ManagementClient managementClient) throws Exception {
            final ModelNode op = new ModelNode();
            op.get(OP_ADDR).set(SUBSYSTEM, "ejb3");
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("client-interceptors");

            final ModelNode value = new ModelNode();

            for (InterceptorModule module : interceptorModules) {
                ModelNode node = new ModelNode();
                node.get(MODULE).set(module.getModuleName());
                node.get("class").set(module.getInterceptorClass().getName());
                value.add(node);
            }

            op.get(VALUE).set(value);
            managementClient.getControllerClient().execute(op);
        }

        @Override
        public void revertClientInterceptors(ManagementClient managementClient) throws Exception {
            final ModelNode op = new ModelNode();
            op.get(OP_ADDR).set(SUBSYSTEM, "ejb3");
            op.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("client-interceptors");

            final ModelNode operationResult = managementClient.getControllerClient().execute(op);
            // check whether the operation was successful
            assertTrue(Operations.isSuccessfulOutcome(operationResult));
        }


        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            if(s.equals("multinode-client")) {
                for (InterceptorModule module : interceptorModules) {
                    packModule(module);
                }
                modifyClientInterceptors(interceptorModules, managementClient);
                // reload in order to apply server-interceptors changes
                ServerReload.executeReloadAndWaitForCompletion(managementClient);
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            if(s.equals("multinode-client")) {
                for (InterceptorModule module: interceptorModules) {
                    module.getTestModule().remove();
                }
                revertClientInterceptors(managementClient);
                // reload in order to apply server-interceptors changes
                ServerReload.executeReloadAndWaitForCompletion(managementClient);
            }
        }
    }
}
