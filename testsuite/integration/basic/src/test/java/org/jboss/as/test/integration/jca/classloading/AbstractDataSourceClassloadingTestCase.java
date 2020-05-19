package org.jboss.as.test.integration.jca.classloading;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.datasource.Datasource;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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

        protected Setup(String classNamePropertyName, String driverClass) {
            this.classNamePropertyName = classNamePropertyName;
            this.driverClass = driverClass;
        }

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            snapshot = ServerSnapshot.takeSnapshot(managementClient);

            setupModule();
            setupDriver(managementClient, classNamePropertyName, driverClass);
            setupDs(managementClient, "TestDS", false);
            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            snapshot.close();
            File testModuleRoot = new File(getModulePath(), "org/jboss/test/testDriver");
            if (testModuleRoot.exists()) {
                deleteRecursively(testModuleRoot);
            }
            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);
        }

        private void setupModule() throws IOException {
            File testModuleRoot = new File(getModulePath(), "org/jboss/test/testDriver");
            File file = new File(testModuleRoot, "main");
            if (file.exists()) {
                deleteRecursively(file);
            }
            if (!file.mkdirs()) {
                // TODO handle
            }

            try(FileOutputStream jarFile = new FileOutputStream(new File(file, "module.xml"));
                PrintWriter pw = new PrintWriter(jarFile)) {
                pw.println("<module name=\"org.jboss.test.testDriver\" xmlns=\"urn:jboss:module:1.8\">\n" +
                        "    <resources>\n" +
                        "        <resource-root path=\"testDriver.jar\"/>\n" +
                        "    </resources>\n" +
                        "\n" +
                        "    <dependencies>\n" +
                        "        <module name=\"javax.sql.api\"/>\n" +
                        "        <module name=\"sun.jdk\"/>\n" +
                        "        <module name=\"javax.orb.api\"/>\n" +
                        "        <module name=\"java.logging\"/>\n" +
                        "    </dependencies>\n" +
                        "</module>");
            }


            JavaArchive deployment = getDeployment();
            try(FileOutputStream jarFile = new FileOutputStream(new File(file, "testDriver.jar"))) {
                deployment.as(ZipExporter.class).exportTo(jarFile);
                jarFile.flush();
            }
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

        private static File getModulePath() {
            String modulePath = System.getProperty("module.path", null);
            if (modulePath == null) {
                String jbossHome = System.getProperty("jboss.home", null);
                if (jbossHome == null) {
                    throw new IllegalStateException("Neither -Dmodule.path nor -Djboss.home were set");
                }
                modulePath = jbossHome + File.separatorChar + "modules";
            } else {
                modulePath = modulePath.split(File.pathSeparator)[0];
            }
            File moduleDir = new File(modulePath);
            if (!moduleDir.exists()) {
                throw new IllegalStateException("Determined module path does not exist");
            }
            if (!moduleDir.isDirectory()) {
                throw new IllegalStateException("Determined module path is not a dir");
            }
            return moduleDir;
        }

        private void setupDriver(ManagementClient managementClient, String classNamePropertyName, String driverClass) throws Exception {
            ModelNode address = new ModelNode();
            address.add("subsystem", "datasources");
            address.add("jdbc-driver", "test");

            ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("driver-module-name").set("org.jboss.test.testDriver");
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
