package org.jboss.as.test.manualmode.jca.connectionlistener;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.deployment.AbstractModuleDeployment;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.manualmode.ejb.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author <a href="mailto:hsvabek@redhat.com">Hynek Svabek</a>
 */
public abstract class AbstractTestsuite extends AbstractModuleDeployment{
    private static final Logger log = Logger.getLogger(AbstractTestsuite.class);

    protected final static String moduleDefaultPath = "system/layers/base/org/jboss/ironjacamar/jdbcadapters/main/";
    
    private static final String JAR_NAME="connlistenerimpl.jar";
    private static final String MODULE_XML_NAME="module.xml";
    private static final String MODULE_XML_BACKUP_SUFFIX=".bac";
    private static final String MODULE_XML_BACKUP_NAME=MODULE_XML_NAME+MODULE_XML_BACKUP_SUFFIX;
    
    protected static final String CONTAINER = "default-jbossas";
    protected static final String DEP_1="connlist_1";
    protected static final String DEP_1_XA="connlist_1_xa";
    protected static final String DEP_2="connlist_2";
    protected static final String DEP_2_XA="connlist_2_xa";
    protected static final String DEP_3="connlist_3";
    protected static final String DEP_3_XA="connlist_3_xa";
    
    private static final String CONNECTION_LISTENER_CLASS_IMPL="org.jboss.as.test.manualmode.jca.connectionlistener.TestConnectionListener";
	
    static final String jndiDs = "java:jboss/datasources/StatDS";
    static final String jndiXaDs = "java:jboss/datasources/StatXaDS";
    
    private File moduleJar;
    
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
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, name+".jar");
        ja.addClasses(ConnectionListenerTestCase.class, AbstractTestsuite.class, JpaTestSlsb.class, JpaTestSlsbRemote.class, MgmtOperationException.class, ContainerResourceMgmtTestBase.class)
                .addAsManifestResource(
                        new StringAsset("Dependencies: org.jboss.dmr \n"), "MANIFEST.MF");
        return ja;
    }
    
    protected static class TestCaseSetup extends ContainerResourceMgmtTestBase implements ServerSetupTask{

    	public static final TestCaseSetup INSTANCE = new TestCaseSetup();
    	
        ModelNode dsAddress;
        ModelNode dsXaAddress;

        @Override
        public void setup(final ManagementClient managementClient, String containerId) throws Exception{
        	setManagementClient(managementClient);
            try {            	
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
         * @param xa - should be data source XA?
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
            operation.get("enabled").set("false");
            if (!xa)
                operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            operation.get("prepared-statements-cache-size").set(3);
            operation.get("user-name").set("sa");
            operation.get("password").set("sa");
            
            
            operation.get("connection-listener-class").set(CONNECTION_LISTENER_CLASS_IMPL);
            executeOperation(operation);
                   
            if (xa) {
                final ModelNode xaDatasourcePropertiesAddress = address.clone();
                xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
                xaDatasourcePropertiesAddress.protect();
                final ModelNode xaDatasourcePropertyOperation = new ModelNode();
                xaDatasourcePropertyOperation.get(OP).set("add");
                xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
                xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

                executeOperation(xaDatasourcePropertyOperation);
            }

            operation = new ModelNode();
            operation.get(OP).set("enable");
            operation.get(OP_ADDR).set(address);

            executeOperation(operation);

            return address;
        }
    }
    
    protected void modifyModuleIronJacamarJdbc() throws Exception{
    	addModule(moduleDefaultPath);
    	createSubModuleJarConnectionListenerImpl(JAR_NAME);
    	
    	//we have to edit module.xml and add our new JAR as resource
    	
    	File moduleXml = new File(testModuleRoot, MODULE_XML_NAME);
    	if(moduleXml.exists() && moduleXml.isFile()){
    		File backupModuleXml = new File(testModuleRoot, MODULE_XML_BACKUP_NAME);
    		copyFile(backupModuleXml, new FileInputStream(moduleXml));
    		
    		try(BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(backupModuleXml)));
    				PrintWriter out = new PrintWriter(new File(testModuleRoot, MODULE_XML_NAME));) {
    			String line;
    			while ((line = in.readLine()) != null) {
    				out.println(line);
    				
    				if(line.trim().startsWith("<resource-root")){
    					out.println("<resource-root path=\""+JAR_NAME+"\"/>");
    				}
    			}
    		}
    	}else{
    		throw new IllegalStateException(MODULE_XML_NAME + " must exists");
    	}
    	
    	
    	
    }    
    
    public void restoreModuleXml(){
    	try{
    	File moduleBackupXml = new File(testModuleRoot, MODULE_XML_BACKUP_NAME);
    	if(moduleBackupXml.exists() && moduleBackupXml.isFile()){
    		File moduleXml = new File(testModuleRoot, MODULE_XML_NAME);
    		moduleXml.delete();
    		moduleBackupXml.renameTo(new File(testModuleRoot, MODULE_XML_NAME));
    	}
    	}catch(Exception e){
    		log.warn(e.getMessage());
    	}
    	
    	try {
    		if(moduleJar != null){
    			moduleJar.delete();
    		}
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
    }

    public void addModule(final String moduleName) throws Exception {
        testModuleRoot = new File(getModulePath(), moduleName);
    }

    protected void createSubModuleJarConnectionListenerImpl(String jarName) throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, jarName);
        jar.addClass(
                TestConnectionListener.class);
        
//        jar.as(ExplodedExporter.class).exportExploded(testModuleRoot, "main");
        moduleJar = new File(testModuleRoot, JAR_NAME);
        copyFile(moduleJar, jar.as(ZipExporter.class).exportAsInputStream());
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
    
    @Override
	protected void fillModuleWithFlatClasses(String raFile) throws Exception {
		
	}

	@Override
	protected void fillModuleWithJar(String raFile) throws Exception {
		
	}

	@Override
	protected String getSlot() {
		return null;
	}
}
