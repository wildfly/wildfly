/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.naming.connector;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * Tests that JMX Connector work properly from container.
 *
 * @author baranowb
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JMXConnectorTestCase {
    // NOTE: this test may fail on Ubuntu, since it has definition of localhost as 127.0.1.1
    protected final Logger log = Logger.getLogger(getClass().getName());
    protected static boolean DONE = false;
    protected static final String CB_DEPLOYMENT_NAME = "naming-connector-bean";
    protected static final String CB_MODULE = "jmxrmiconnector";
    protected ConnectedBeanInterface connectedBean;

    @ArquillianResource
    protected ManagementClient managementClient;
    @ArquillianResource
    public Deployer deployer;

    @Deployment(managed = false, name = CB_DEPLOYMENT_NAME)
    public static JavaArchive createTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CB_DEPLOYMENT_NAME);
        archive.addClass(ConnectedBean.class);
        archive.addClass(ConnectedBeanInterface.class);
        archive.addAsManifestResource(new StringAsset("Dependencies: org.jboss.xnio," + CB_MODULE + "\n"), "MANIFEST.MF");
        return archive;
    }

    @Test
    public void testLookup() throws Exception {
        this.connectedBean.testConnector(getJMXURI());

        try {
            this.connectedBean.testConnector(getBadJMXURI());
            Assert.assertTrue("Lookup should fail!", false);
        } catch (IOException ioe) {
            // code throws IOE....
            Assert.assertTrue("Wrong cause", ioe.getCause() != null);
            Assert.assertTrue("Wrong cause", ioe.getCause() instanceof NamingException);
            NamingException cause = (NamingException) ioe.getCause();
//            this will fail always, since return value has escape chars
//            NamingException expected = NamingMessages.MESSAGES.noURLContextFactory(getBadURLPart());
//            Assert.assertEquals("Wrong cause message",expected.getMessage(), cause.getMessage());
            Assert.assertTrue(cause.getMessage().contains("Context.URL_PKG_PREFIXES"));
        }
    }

    
    protected JMXConnectorServer connectorServer;

    @Before
    public void beforeTest() throws Exception {
        createRegistry();
        lookupBean();
        createModule();
        deploy();
    }

    @After
    public void afterTest() throws Exception {
        deleteModule();
        undeploy();
        if(this.connectorServer!=null){
            this.connectorServer.stop();
        }
    }

    protected void deploy() {
        deployer.deploy(CB_DEPLOYMENT_NAME);
    }

    protected void undeploy() {
        deployer.undeploy(CB_DEPLOYMENT_NAME);

    }

    protected void createModule() throws IOException {
        final File testModuleRoot = getTestModulePath();
        if (testModuleRoot.exists()) {
            throw new IllegalArgumentException(testModuleRoot + " already exists");
        }
        File file = new File(testModuleRoot, "main").getCanonicalFile();
        if (!file.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + file);
        }

        URL url = this.getClass().getResource("module.xml");
        if (url == null) {
            throw new IllegalStateException("Could not find module.xml");
        }
        copyFile(new File(file, "module.xml"), url.openStream());

        final ModelControllerClient client = managementClient.getControllerClient();
        executeOperation(client, ADD, PathAddress.pathAddress(PathElement.pathElement(EXTENSION, CB_MODULE)), false);
    }

    protected void deleteModule() {
        deleteRecursively(getTestModulePath());

    }

    protected void deleteRecursively(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (String name : file.list()) {
                    deleteRecursively(new File(file, name));
                }
            }
            file.delete();
        }
    }

    protected File getModulePath() {
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

    protected File getTestModulePath() {
        return new File(getModulePath(), CB_MODULE);
    }

    protected void copyFile(File target, InputStream src) throws IOException {
        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
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

    protected ModelNode executeOperation(ModelControllerClient client, String name, PathAddress address, boolean fail)
            throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP).set(name);
        op.get(OP_ADDR).set(address.toModelNode());

        ModelNode result = client.execute(op);
        if (!fail) {
            Assert.assertFalse(result.toString(), result.get(FAILURE_DESCRIPTION).isDefined());
        } else {
            Assert.assertTrue(result.get(FAILURE_DESCRIPTION).isDefined());
        }

        return result.get(RESULT);
    }

    protected void createRegistry() throws Exception {
        if (DONE) {
            return;
        }
        LocateRegistry.createRegistry(getPort());
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JMXServiceURL url = new JMXServiceURL(getJMXURI());
        this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        this.connectorServer.start();
        DONE = true;
    }

    protected void lookupBean() throws Exception {
        InitialContext initialContext = getInitialContext();
        final Object value = initialContext.lookup("ejb:/" + CB_DEPLOYMENT_NAME + "/" + ConnectedBean.class.getSimpleName()
                + "!" + ConnectedBeanInterface.class.getName());
        Assert.assertNotNull("Failed to lookup EJB!", value);
        Assert.assertTrue("", value instanceof ConnectedBeanInterface);
        this.connectedBean = (ConnectedBeanInterface) value;
    }

    protected String getJMXURI() {
        return "service:jmx:rmi:///jndi/rmi://" + getHost() + ":" + getPort() + "/jmxrmi";
    }

    protected String getBadJMXURI() {
        return "service:jmx:rmi:///jndi/"+getBadURLPart();
    }

    protected String getBadURLPart() {
        return "rmi2://" + getHost() + ":" + getPort() + "/jmxrmi2";
    }

    
    protected String getHost() {
        //return managementClient.getMgmtAddress();
        return "127.0.0.1";
    }

    protected int getPort() {
        return 1099;
    }

    protected InitialContext getInitialContext() throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<String, String>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(jndiProperties);
    }
}