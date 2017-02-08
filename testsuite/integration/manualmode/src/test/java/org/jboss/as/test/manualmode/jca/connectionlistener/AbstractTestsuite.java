package org.jboss.as.test.manualmode.jca.connectionlistener;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.manualmode.ejb.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.xnio.IoUtils;

/**
 * @author <a href="mailto:hsvabek@redhat.com">Hynek Svabek</a>
 */
public abstract class AbstractTestsuite {
    private static final Logger log = Logger.getLogger(AbstractTestsuite.class);

    protected static final String MODULE_NAME = "connlistener";

    protected static final String CONTAINER = "default-jbossas";
    protected static final String DEP_1 = "connlist_1";
    protected static final String DEP_1_XA = "connlist_1_xa";
    protected static final String DEP_2 = "connlist_2";
    protected static final String DEP_2_XA = "connlist_2_xa";
    protected static final String DEP_3 = "connlist_3";
    protected static final String DEP_3_XA = "connlist_3_xa";

    private static final String CONNECTION_LISTENER_CLASS_IMPL = TestConnectionListener.class.getName();

    static final String jndiDs = "java:jboss/datasources/StatDS";
    static final String jndiXaDs = "java:jboss/datasources/StatXaDS";

    protected Context context;

    @Deployment(name = DEP_1, managed = false)
    public static JavaArchive createDeployment1() throws Exception {
        return createDeployment(DEP_1);
    }

    @Deployment(name = DEP_1_XA, managed = false)
    public static JavaArchive createDeployment1Xa() throws Exception {
        return createDeployment(DEP_1_XA);
    }

    @Deployment(name = DEP_2, managed = false)
    public static JavaArchive createDeployment2() throws Exception {
        return createDeployment(DEP_2);
    }

    @Deployment(name = DEP_2_XA, managed = false)
    public static JavaArchive createDeployment2Xa() throws Exception {
        return createDeployment(DEP_2_XA);
    }

    @Deployment(name = DEP_3, managed = false)
    public static JavaArchive createDeployment3() throws Exception {
        return createDeployment(DEP_3);
    }

    @Deployment(name = DEP_3_XA, managed = false)
    public static JavaArchive createDeployment3Xa() throws Exception {
        return createDeployment(DEP_3_XA);
    }

    public static JavaArchive createDeployment(String name) throws Exception {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, name + ".jar");
        ja.addClasses(ConnectionListenerTestCase.class, AbstractTestsuite.class, JpaTestSlsb.class, JpaTestSlsbRemote.class, MgmtOperationException.class, ContainerResourceMgmtTestBase.class)
                .addAsManifestResource(
                        new StringAsset("Dependencies: org.jboss.dmr \n"), "MANIFEST.MF");
        return ja;
    }

    protected static class TestCaseSetup extends ContainerResourceMgmtTestBase implements ServerSetupTask {

        public static final TestCaseSetup INSTANCE = new TestCaseSetup();

        ModelNode dsAddress;
        ModelNode dsXaAddress;

        @Override
        public void setup(final ManagementClient managementClient, String containerId) throws Exception {
            setManagementClient(managementClient);
            try {
                tryRemoveConnListenerModule();//before test, in tearDown we cannot connect to CLI....
                addConnListenerModule();
                dsAddress = createDataSource(false, jndiDs);
                dsXaAddress = createDataSource(true, jndiXaDs);
            } catch (Throwable e) {
                removeDss();
                throw new Exception(e);
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            removeDss();
        }

        public void removeDss() {
            try {
                remove(dsAddress);
            } catch (Throwable e) {
                log.warn(e.getMessage());
            }
            try {
                remove(dsXaAddress);
            } catch (Throwable e) {
                log.warn(e.getMessage());
            }
        }

        /**
         * Creates data source and return its node address
         *
         * @param xa       - should be data source XA?
         * @param jndiName of data source
         * @return ModelNode - address of data source node
         * @throws Exception
         */
        private ModelNode createDataSource(boolean xa, String jndiName) throws Exception {
            ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, "datasources");
            address.add((xa ? "xa-" : "") + "data-source", jndiName);
            address.protect();

            ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("jndi-name").set(jndiName);
            operation.get("driver-name").set("h2");
            operation.get("prepared-statements-cache-size").set(3);
            operation.get("user-name").set("sa");
            operation.get("password").set("sa");

            if (!xa) {
                operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            }

            operation.get("connection-listener-class").set(CONNECTION_LISTENER_CLASS_IMPL);
            operation.get("connection-listener-property").set("testString", "MyTest");
            /**
             * Uncomment after finishing task https://issues.jboss.org/browse/WFLY-2492
             */
//            operation.get("connection-listener-module-name").set(MODULE_NAME);
//            operation.get("connection-listener-slot").set("main");
            operation.get("enabled").set("false");
            executeOperation(operation);

            if (xa) {
                final ModelNode xaDatasourcePropertiesAddress = address.clone();
                xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
                xaDatasourcePropertiesAddress.protect();
                final ModelNode xaDatasourcePropertyOperation = new ModelNode();
                xaDatasourcePropertyOperation.get(OP).set("add");
                xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
                xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
                executeOperation(xaDatasourcePropertyOperation);
            }


            operation = new ModelNode();
            operation.get(OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("enabled");
            operation.get("value").set("true");
            executeOperation(operation);

            ServerReload.executeReloadAndWaitForCompletion(getModelControllerClient(), 50000);

            return address;
        }

        protected void addConnListenerModule() throws Exception {
            JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
            jar.addClasses(TestConnectionListener.class)
                    .addPackage(Logger.class.getPackage());

            File moduleJar = File.createTempFile(MODULE_NAME, ".jar");
            copyModuleFile(moduleJar, jar.as(ZipExporter.class).exportAsInputStream());

            CLIWrapper cli = new CLIWrapper(true);
            cli.sendLine("module add --name=" + MODULE_NAME + " --slot=main --dependencies=org.jboss.ironjacamar.jdbcadapters --resources=" + moduleJar.getAbsolutePath());
            /*
             * Delete global module after finishing this task
             * https://issues.jboss.org/browse/WFLY-2492
             */
            cli.sendLine("/subsystem=ee:write-attribute(name=global-modules,value=[{name => " + MODULE_NAME + ",slot => main}]");
            cli.quit();
        }

        protected void tryRemoveConnListenerModule() throws Exception {
            try {
                CLIWrapper cli = new CLIWrapper(true);
                cli.sendLine("module remove --name=" + MODULE_NAME + " --slot=main");
                cli.quit();
            } catch (Throwable e) {
                log.warn(e.getMessage());
            }
        }
    }

    protected static void copyModuleFile(File target, InputStream src) throws IOException {
        final BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(target));
        try {
            int i = src.read();
            while (i != -1) {
                out.write(i);
                i = src.read();
            }
        } finally {
            IoUtils.safeClose(out);
        }
    }

    protected <T> T lookup(final Class<T> remoteClass, final Class<?> beanClass, final String archiveName) throws NamingException {
        String myContext = Util.createRemoteEjbJndiContext(
                "",
                archiveName,
                "",
                beanClass.getSimpleName(),
                remoteClass.getName(),
                false);

        return remoteClass.cast(context.lookup(myContext));
    }
}
