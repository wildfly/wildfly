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

package org.wildfly.test.integration.microprofile.config.smallrye;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.integration.microprofile.config.smallrye.app.MicroProfileConfigTestCase;

/**
 * Add a config-source with a property class in the microprofile-config-smallrye subsystem.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SubsystemConfigSourceTask implements ServerSetupTask {
    public static final String MY_PROP_FROM_SUBSYSTEM_PROP_NAME = "my.prop.from.subsystem";
    public static final String MY_PROP_FROM_SUBSYSTEM_PROP_VALUE = "I'm configured in the subsystem";

    public static final String BOOL_OVERRIDDEN_PROP_NAME = "boolOverridden";
    public static final String BOOLEAN_OVERRIDDEN_PROP_NAME = "booleanOverridden";
    public static final String BOOLEAN_OVERRIDDEN_PROP_VALUE = "yes";

    public static final String INT_OVERRIDDEN_PROP_NAME = "intOverridden";
    public static final String INTEGER_OVERRIDDEN_PROP_NAME = "integerOverridden";
    public static final String INTEGER_OVERRIDDEN_PROP_VALUE = String.valueOf(Integer.MAX_VALUE);

    public static final String LONG_OVERRIDDEN_PROP_NAME = "longOverridden";
    public static final String LONG_CLASS_OVERRIDDEN_PROP_NAME = "longClassOverridden";
    public static final String LONG_OVERRIDDEN_PROP_VALUE = String.valueOf(Long.MAX_VALUE);

    public static final String FLOAT_OVERRIDDEN_PROP_NAME = "floatOverridden";
    public static final String FLOAT_CLASS_OVERRIDDEN_PROP_NAME = "floatClassOverridden";
    public static final String FLOAT_OVERRIDDEN_PROP_VALUE = String.valueOf(Float.MAX_VALUE);

    public static final String DOUBLE_OVERRIDDEN_PROP_NAME = "doubleOverridden";
    public static final String DOUBLE_CLASS_OVERRIDDEN_PROP_NAME = "doubleClassOverridden";
    public static final String DOUBLE_OVERRIDDEN_PROP_VALUE = String.valueOf(Double.MAX_VALUE);

    public static final String PROPERTIES_PROP_NAME0 = "priority.prop.0";
    public static final String PROPERTIES_PROP_NAME1 = "priority.prop.1";
    public static final String PROPERTIES_PROP_NAME2 = "priority.prop.2";
    public static final String PROPERTIES_PROP_NAME3 = "priority.prop.3";
    public static final String PROPERTIES_PROP_NAME4 = "priority.prop.4";
    public static final String PROPERTIES_PROP_NAME5 = "priority.prop.5";

    public static final String PROP1_VALUE = "priority.prop.1 value loaded via properties config-source";
    public static final String PROP2_VALUE = "priority.prop.2 value loaded via properties config-source";
    public static final String PROP3_VALUE = "priority.prop.3 value loaded via properties config-source";
    public static final String PROP4_VALUE = "priority.prop.4 value loaded via properties config-source";

//    public static final String CUSTOM_FILE_PROPERTY_NAME = "custom.file.property";

    public static final String MICROPROFILE_SUBSYSTEM_NAME = "microprofile-config-smallrye";
    public static final String CONFIG_SOURCE = "config-source";


    // Contains list of created config sources --- this is used during tearDown
    private LinkedList<String> registeredConfigSources = new LinkedList<>();

    /* Default ordinal values, https://github.com/eclipse/microprofile-config#design
        - System.getProperties() (ordinal=400)
        - System.getenv() (ordinal=300)
        - all META-INF/microprofile-config.properties files on the ClassPath. (default ordinal=100, separately
          configurable via a config_ordinal property inside each file)
     */

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient mgmtCli = managementClient.getControllerClient();

        // TODO system properties and env properites should be added here...

        // ===== PROPERTIES TYPE config sources (ordinal default is 100) =====
        addPropertiesConfigSource(mgmtCli, BOOL_OVERRIDDEN_PROP_NAME, BOOL_OVERRIDDEN_PROP_NAME, BOOLEAN_OVERRIDDEN_PROP_VALUE);
        addPropertiesConfigSource(mgmtCli, BOOLEAN_OVERRIDDEN_PROP_NAME, BOOLEAN_OVERRIDDEN_PROP_NAME, BOOLEAN_OVERRIDDEN_PROP_VALUE);

        addPropertiesConfigSource(mgmtCli, INT_OVERRIDDEN_PROP_NAME, INT_OVERRIDDEN_PROP_NAME, INTEGER_OVERRIDDEN_PROP_VALUE);
        addPropertiesConfigSource(mgmtCli, INTEGER_OVERRIDDEN_PROP_NAME, INTEGER_OVERRIDDEN_PROP_NAME, INTEGER_OVERRIDDEN_PROP_VALUE);

        addPropertiesConfigSource(mgmtCli, LONG_OVERRIDDEN_PROP_NAME, LONG_OVERRIDDEN_PROP_NAME, LONG_OVERRIDDEN_PROP_VALUE);
        addPropertiesConfigSource(mgmtCli, LONG_CLASS_OVERRIDDEN_PROP_NAME, LONG_CLASS_OVERRIDDEN_PROP_NAME, LONG_OVERRIDDEN_PROP_VALUE);

        addPropertiesConfigSource(mgmtCli, FLOAT_OVERRIDDEN_PROP_NAME, FLOAT_OVERRIDDEN_PROP_NAME, FLOAT_OVERRIDDEN_PROP_VALUE);
        addPropertiesConfigSource(mgmtCli, FLOAT_CLASS_OVERRIDDEN_PROP_NAME, FLOAT_CLASS_OVERRIDDEN_PROP_NAME, FLOAT_OVERRIDDEN_PROP_VALUE);

        addPropertiesConfigSource(mgmtCli, DOUBLE_OVERRIDDEN_PROP_NAME, DOUBLE_OVERRIDDEN_PROP_NAME, DOUBLE_OVERRIDDEN_PROP_VALUE);
        addPropertiesConfigSource(mgmtCli, DOUBLE_CLASS_OVERRIDDEN_PROP_NAME, DOUBLE_CLASS_OVERRIDDEN_PROP_NAME, DOUBLE_OVERRIDDEN_PROP_VALUE);

        addPropertiesConfigSource(mgmtCli, "petsProperty", "myPetsOverridden", "donkey,shrek\\,fiona");

        addPropertiesConfigSource(mgmtCli, "propertiesProp", MY_PROP_FROM_SUBSYSTEM_PROP_NAME, MY_PROP_FROM_SUBSYSTEM_PROP_VALUE);

        addPropertiesConfigSource(mgmtCli, "propertiesProp1", PROPERTIES_PROP_NAME1, PROP1_VALUE);
        addPropertiesConfigSource(mgmtCli, "propertiesProp2", PROPERTIES_PROP_NAME2, PROP2_VALUE, 200);
        addPropertiesConfigSource(mgmtCli, "propertiesProp3", PROPERTIES_PROP_NAME3, PROP3_VALUE, 350);

        // ===== DIR TYPE config sources (ordinal default is 100) =====

//        File propertiesDir = new File(MicroProfileConfigTestCase.class.getResource(CUSTOM_FILE_PROPERTY_NAME).toURI()).getParentFile();
        // TODO workaround due to the https://issues.jboss.org/browse/WFWIP-57
        File propertiesDir = new File(MicroProfileConfigTestCase.class.getResource("microprofile-config.properties").toURI()).getParentFile();
        addDirConfigSource(mgmtCli, "dirProp1", propertiesDir.getAbsolutePath() + File.separator + "fileProperty1");
        addDirConfigSource(mgmtCli, "dirProp2", propertiesDir.getAbsolutePath() + File.separator + "fileProperty2", 200);
        addDirConfigSource(mgmtCli, "dirProp3", propertiesDir.getAbsolutePath() + File.separator + "fileProperty3", 350);
        addDirConfigSource(mgmtCli, "dirProp4", propertiesDir.getAbsolutePath() + File.separator + "fileProperty4", 450);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient mgmtCli = managementClient.getControllerClient();
        removeConfigSource(mgmtCli, registeredConfigSources);
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
     * Adds config-source of type 'dir' with given value and priority ordinal value set to default.
     *
     * @param client           model controller client
     * @param configSourceName name of the added config-source resource
     * @param dirPath          value of the config-source - path to the directory from where the properties should be
     *                         loaded
     * @throws IOException
     */
    private void addDirConfigSource(ModelControllerClient client, String configSourceName, String dirPath) throws
            IOException {
        addDirConfigSource(client, configSourceName, dirPath, -1);
    }

    /**
     * Adds config-source of type 'dir' with given value and priority ordinal value.
     *
     * @param client           model controller client
     * @param configSourceName name of the added config-source resource
     * @param dirPath          value of the config-source - path to the directory from where the properties should be
     *                         loaded
     * @param ordinal          ordinal value of the priority of the added config-source; if less than zero, default is
     *                         set
     * @throws IOException
     */
    private void addDirConfigSource(ModelControllerClient client, String configSourceName, String dirPath,
            int ordinal) throws IOException {
        ModelNode op;
        op = new ModelNode();
        op.get(OP_ADDR).add(SUBSYSTEM, MICROPROFILE_SUBSYSTEM_NAME);
        op.get(OP_ADDR).add(CONFIG_SOURCE, configSourceName);
        op.get(OP).set(ADD);

        ModelNode dir = new ModelNode();
        dir.get("path").set(dirPath);

        op.get("dir").set(dir);

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
