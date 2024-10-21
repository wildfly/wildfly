/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.classloading;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.datasource.Datasource;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

public abstract class AbstractDataSourceClassloadingTestCase {

    public static JavaArchive getDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "driver.jar");
        jar.addClass(ClassloadingDataSource.class);
        jar.addClass(ClassloadingDriver.class);
        jar.addClass(TestConnection.class);
        jar.addClass(ClassloadingXADataSource.class);
        jar.addClass(TestXAConnection.class);
        jar.addAsServiceProvider(Driver.class, ClassloadingDriver.class);
        return jar;
    }

    @Deployment
    public static JavaArchive getTesterDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "tester.jar");
        jar.addClass(AbstractDataSourceClassloadingTestCase.class);
        jar.addClass(DataSourceTcclDatasourceClassTestCase.class);
        return jar;
    }

    @Resource(mappedName = "java:jboss/datasources/TestDS")
    private DataSource ds;

    public static class Setup implements ServerSetupTask {

        private AutoCloseable snapshot;
        private String classNamePropertyName;
        private String driverClass;
        private TestModule testModule;

        protected Setup(String classNamePropertyName, String driverClass) {
            this.classNamePropertyName = classNamePropertyName;
            this.driverClass = driverClass;

            String testModuleName = "org.jboss.test.testDriver";
            if (!AssumeTestGroupUtil.isBootableJar()) {
                // use unique module name for each test because test cannot properly delete module in teardown on Windows
                testModuleName = testModuleName + "." + this.getClass().getName();
            }

            testModule = new TestModule(testModuleName,
                    "java.sql", "java.logging", "javax.orb.api");
            JavaArchive driverJar = testModule.addResource("testDriver.jar");
            driverJar.merge(getDeployment());
        }

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            snapshot = ServerSnapshot.takeSnapshot(managementClient);

            if (!AssumeTestGroupUtil.isBootableJar()){
                // skip for bootable jar, module is injected to bootable jar during provisioning, see pom.xml
                setupModule();
            }
            setupDriver(managementClient, classNamePropertyName, driverClass);
            setupDs(managementClient, "TestDS", false);
            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            snapshot.close();
            if (!AssumeTestGroupUtil.isBootableJar()) {
                // skip for bootable jar, module is injected to bootable jar during provisioning, see pom.xml
                testModule.remove();
            }
            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);
        }

        private void setupModule() throws IOException {
            testModule.create();
        }

        private static void deleteRecursively(File file) {
            if (file.exists()) {
                if (file.isDirectory()) {
                    for (String name : file.list()) {
                        deleteRecursively(new File(file, name));
                    }
                }
                file.delete();
            }
        }

        private void setupDriver(ManagementClient managementClient, String classNamePropertyName, String driverClass) throws Exception {
            ModelNode address = new ModelNode();
            address.add("subsystem", "datasources");
            address.add("jdbc-driver", "test");

            ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("driver-module-name").set(testModule.getName());
            operation.get("driver-name").set("test");
            operation.get("driver-datasource-class-name").set("org.jboss.as.test.integration.jca.classloading.ClassloadingDataSource");
            operation.get(classNamePropertyName).set(driverClass);
            managementClient.getControllerClient().execute(operation);
        }

        protected void setupDs(ManagementClient managementClient, String dsName, boolean jta) throws Exception {
            Datasource ds = Datasource.Builder(dsName).build();
            ModelNode address = new ModelNode();
            address.add("subsystem", "datasources");
            address.add("data-source", dsName);

            ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("jndi-name").set(ds.getJndiName());
            operation.get("use-java-context").set("true");
            operation.get("driver-name").set("test");
            operation.get("enabled").set("true");
            operation.get("user-name").set(ds.getUserName());
            operation.get("password").set(ds.getPassword());
            operation.get("jta").set(jta);
            operation.get("use-ccm").set("true");
            operation.get("connection-url").set("jdbc:foo:bar");
            managementClient.getControllerClient().execute(operation);
        }
    }

    @Test
    public void testGetConnection() throws Exception {
        Assert.assertNotNull(ds);
        ds.getConnection();
        ds.getConnection("", "");
    }

    @Test
    public void testCreateStatement() throws Exception {
        Connection connection = ds.getConnection();
        connection.createStatement();
        connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    @Test
    public void testPrepareStatement() throws Exception {
        Connection connection = ds.getConnection();
        connection.prepareStatement("");
        connection.prepareStatement("", new int[]{});
        connection.prepareStatement("", new String[]{});
        connection.prepareStatement("", Statement.RETURN_GENERATED_KEYS);
        connection.prepareStatement("", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        connection.prepareStatement("", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }


}
