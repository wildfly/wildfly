/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source_provider;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.CustomConfigSource;

/**
 * Add a config-source-provider with a custom class in the microprofile-config subsystem.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SetupTask implements ServerSetupTask {
    private static final String MODULE_NAME = "test.custom-config-source-provider";

    private static TestModule testModule;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        URL url = ConfigSourceProviderFromClassTestCase.class.getResource("module.xml");
        File moduleXmlFile = new File(url.toURI());
        testModule = new TestModule(MODULE_NAME, moduleXmlFile);
        testModule.addResource("config-source-provider.jar")
                .addClass(CustomConfigSourceProvider.class)
                .addClass(CustomConfigSource.class);
        testModule.create();

        addConfigSourceProvider(managementClient.getControllerClient());
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        removeConfigSourceProvider(managementClient.getControllerClient());

        testModule.remove();
    }

    private void addConfigSourceProvider(ModelControllerClient client) throws IOException {
        ModelNode op;
        op = new ModelNode();
        op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
        op.get(OP_ADDR).add("config-source-provider", "my-config-source-config_source_provider");
        op.get(OP).set(ADD);
        op.get("class").get("module").set(MODULE_NAME);
        op.get("class").get("name").set(CustomConfigSourceProvider.class.getName());
        client.execute(op);
    }

    private void removeConfigSourceProvider(ModelControllerClient client) throws IOException {
        ModelNode op;
        op = new ModelNode();
        op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
        op.get(OP_ADDR).add("config-source-provider", "my-config-source-config_source_provider");
        op.get(OP).set(REMOVE);
        client.execute(op);
    }
}
