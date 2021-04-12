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

package org.wildfly.test.integration.microprofile.opentracing;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.LinkedList;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

/**
 * Add a config-source with a property class in the microprofile-config-smallrye subsystem.
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
public class ConfigHttpPathTask implements ServerSetupTask {

    public static final String MICROPROFILE_SUBSYSTEM_NAME = "microprofile-config-smallrye";
    public static final String CONFIG_SOURCE = "config-source";
    public static final String MP_OPENTRACING_SKIP_PATTERN = "mp.opentracing.server.skip-pattern";
    public static final String MP_OPENTRACING_SERVER_OPERATION_NAME_PROVIDER = "mp.opentracing.server.operation-name-provider";
    public static final String MP_OPENTRACING_CONFIG = "mpOpentracingConfig";

    // Contains list of created config sources --- this is used during tearDown
    private final LinkedList<String> registeredConfigSources = new LinkedList<>();
    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient mgmtCli = managementClient.getControllerClient();
        addPropertiesConfigSource(mgmtCli, MP_OPENTRACING_CONFIG, MP_OPENTRACING_SERVER_OPERATION_NAME_PROVIDER, "http-path");
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient mgmtCli = managementClient.getControllerClient();
        removeConfigSource(mgmtCli, registeredConfigSources);
        ServerReload.reloadIfRequired(managementClient);
    }

    /**
     * Adds config-source of type 'properties' with given value and priority ordinal value set to default.
     *
     * @param client           model controller client
     * @param configSourceName name of the added config-source resource
     * @param propName         name of the property to be created
     * @param propValue        value of the property which shall be created
     * @throws IOException
     */
    private void addPropertiesConfigSource(ModelControllerClient client, String configSourceName, String propName,
            String propValue) throws IOException {
        addPropertiesConfigSource(client, configSourceName, propName, propValue, -1);
    }

    /**
     * Adds config-source of type 'properties' with given value and priority ordinal value.
     *
     * @param client           model controller client
     * @param configSourceName name of the added config-source resource
     * @param propName         name of the property to be created
     * @param propValue        value of the property which shall be created
     * @param ordinal          ordinal value of the priority of the added config-source; if less than zero, default is
     *                         set
     * @throws IOException
     */
    private void addPropertiesConfigSource(ModelControllerClient client, String configSourceName, String propName,
            String propValue, int ordinal) throws IOException {
        ModelNode op;
        op = new ModelNode();
        op.get(OP_ADDR).add(SUBSYSTEM, MICROPROFILE_SUBSYSTEM_NAME);
        op.get(OP_ADDR).add(CONFIG_SOURCE, configSourceName);
        op.get(OP).set(ADD);
        op.get(PROPERTIES).add(propName, propValue);

        if (ordinal >= 0) {
            op.get("ordinal").set(ordinal);
        }
        client.execute(op);

        registeredConfigSources.add(configSourceName);
    }

    /**
     * Removes defined list of config-sources.
     *
     * @param client model controller client
     * @param names  list of strings containing names of the config-sources to be removed
     * @throws IOException
     */
    private void removeConfigSource(ModelControllerClient client, LinkedList<String> names) throws IOException {
        for (String name : names) {
            removeConfigSource(client, name);
        }
    }

    /**
     * Removes defined config-source.
     *
     * @param client model controller client
     * @param name   string containing name of the config-source to be removed
     * @throws IOException
     */
    private void removeConfigSource(ModelControllerClient client, String name) throws IOException {
        ModelNode op;
        op = new ModelNode();
        op.get(OP_ADDR).add(SUBSYSTEM, MICROPROFILE_SUBSYSTEM_NAME);
        op.get(OP_ADDR).add(CONFIG_SOURCE, name);
        op.get(OP).set(REMOVE);
        client.execute(op);
    }
}
