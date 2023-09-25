/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.classloading;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.datasource.Datasource;
import org.jboss.dmr.ModelNode;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

@RunWith(Arquillian.class)
@ServerSetup(DataSourceTcclXADatasourceClassTestCase.Setup.class)
public class DataSourceTcclXADatasourceClassTestCase extends AbstractDataSourceClassloadingTestCase {

    public static class Setup extends AbstractDataSourceClassloadingTestCase.Setup {

        public Setup() {
            super("driver-xa-datasource-class-name", ClassloadingXADataSource.class.getName());
        }

        protected void setupDs(ManagementClient managementClient, String dsName, boolean jta) throws Exception {
            Datasource ds = Datasource.Builder(dsName).build();
            ModelNode address = new ModelNode();
            address.add("subsystem", "datasources");
            address.add("xa-data-source", dsName);

            ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("jndi-name").set(ds.getJndiName());
            operation.get("use-java-context").set("true");
            operation.get("driver-name").set("test");
            operation.get("enabled").set("false");
            operation.get("user-name").set(ds.getUserName());
            operation.get("password").set(ds.getPassword());
            managementClient.getControllerClient().execute(operation);

            ModelNode prop = new ModelNode();
            prop.get(OP).set(ADD);
            ModelNode propAddress = address.clone();
            propAddress = propAddress.add("xa-datasource-properties", "URL");
            prop.get(OP_ADDR).set(propAddress);
            prop.get("value").set("foo:bar");
            managementClient.getControllerClient().execute(prop);

            ModelNode attr = new ModelNode();
            attr.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            attr.get(OP_ADDR).set(address);
            attr.get(NAME).set("enabled");
            attr.get(VALUE).set("true");
            managementClient.getControllerClient().execute(attr);
        }
    }
}
