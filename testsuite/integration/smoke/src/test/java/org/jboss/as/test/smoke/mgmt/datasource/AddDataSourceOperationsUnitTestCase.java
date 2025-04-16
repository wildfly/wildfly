/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.mgmt.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.sql.Driver;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.connector.subsystems.datasources.Namespace;
import org.jboss.as.test.integration.management.jca.DsMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Datasource operation unit test.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient

public class AddDataSourceOperationsUnitTestCase extends DsMgmtTestBase{

    private static final String JDBC_DRIVER_NAME = "test-jdbc.jar";

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, JDBC_DRIVER_NAME)
                .addClass(TestDriver.class)
                .addAsServiceProvider(Driver.class, TestDriver.class)
                // simulate a driver that includes a main class, as WF would treat such a deployment
                // as an appclient module. So see what happens.
                .setManifest(new StringAsset("Main-Class: " + TestDriver.class.getName()));
    }

    @Test
    public void testAddDsAndTestConnection() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("data-source", "MySqlDs_Pool");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        operation.get("jndi-name").set("java:jboss/datasources/MySqlDs");
        operation.get("driver-name").set(JDBC_DRIVER_NAME);
        operation.get("connection-url").set("dont_care");
        operation.get("user-name").set("sa");
        operation.get("password").set("sa");

        executeOperation(operation);

        final ModelNode operation0 = new ModelNode();
        operation0.get(OP).set("take-snapshot");

        executeOperation(operation0);

        List<ModelNode> newList = marshalAndReparseDsResources("data-source");

        remove(address);

        Assert.assertNotNull("Reparsing failed:",newList);

        Assert.assertNotNull(findNodeWithProperty(newList,"jndi-name","java:jboss/datasources/MySqlDs"));
   }

    private List<ModelNode> marshalAndReparseDsResources(String childType) throws Exception {
        DataSourcesExtension.DataSourceSubsystemParser parser = new DataSourcesExtension.DataSourceSubsystemParser();
        return xmlToModelOperations(modelToXml("datasources", childType, parser), Namespace.CURRENT.getUriString(), parser);
    }

}
