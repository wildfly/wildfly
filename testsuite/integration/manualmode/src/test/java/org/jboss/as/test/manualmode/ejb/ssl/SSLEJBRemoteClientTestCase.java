/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.ejb.ssl;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.manualmode.ejb.ssl.beans.StatefulBean;
import org.jboss.as.test.manualmode.ejb.ssl.beans.StatefulBeanRemote;
import org.jboss.as.test.manualmode.ejb.ssl.beans.StatelessBean;
import org.jboss.as.test.manualmode.ejb.ssl.beans.StatelessBeanRemote;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Testing ssl connection of remote ejb client.
 *
 * @author Ondrej Chaloupka
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("WFLY-1836")
public class SSLEJBRemoteClientTestCase {
    private static final Logger log = Logger.getLogger(SSLEJBRemoteClientTestCase.class);
    private static final String MODULE_NAME_STATELESS = "ssl-remote-ejb-client-test";
    private static final String MODULE_NAME_STATEFUL = "ssl-remote-ejb-client-test-stateful";
    public static final String DEPLOYMENT_STATELESS = "dep_stateless";
    public static final String DEPLOYMENT_STATEFUL = "dep_stateful";
    private static boolean serverConfigDone = false;
    private static ContextSelector<EJBClientContext> previousClientContextSelector;

    @ArquillianResource
    private static ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    public static final String DEFAULT_JBOSSAS = "default-jbossas";

    @Deployment(name = DEPLOYMENT_STATELESS, managed = false)
    @TargetsContainer(DEFAULT_JBOSSAS)
    public static Archive<?> deployStateless() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_STATELESS + ".jar");
        jar.addClasses(StatelessBeanRemote.class, StatelessBean.class);
        return jar;
    }

    @Deployment(name = DEPLOYMENT_STATEFUL, managed = false)
    @TargetsContainer(DEFAULT_JBOSSAS)
    public static Archive<?> deployStateful() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_STATEFUL + ".jar");
        jar.addClasses(StatefulBeanRemote.class, StatefulBean.class);
        return jar;
    }

    @BeforeClass
    public static void prepare() throws Exception {
        log.info("*** BEFORE CLASS ***");
        log.info("*** javax.net.ssl.trustStore="+System.getProperty("javax.net.ssl.trustStore"));
        log.info("*** javax.net.ssl.trustStorePassword="+System.getProperty("javax.net.ssl.trustStorePassword"));
        log.info("*** javax.net.ssl.keyStore="+System.getProperty("javax.net.ssl.keyStore"));
        log.info("*** javax.net.ssl.keyStorePassword="+System.getProperty("javax.net.ssl.keyStorePassword"));
        // probably not required, we get these properties from maven
        /*System.setProperty("javax.net.ssl.trustStore", client_truststore_path);
        System.setProperty("javax.net.ssl.trustStorePassword", SSLRealmSetupTool.CLIENT_KEYSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.keyStore", client_keystore_path);
        System.setProperty("javax.net.ssl.keyStorePassword", SSLRealmSetupTool.CLIENT_KEYSTORE_PASSWORD);*/
        System.setProperty("jboss.ejb.client.properties.skip.classloader.scan", "true");
    }

    private static ContextSelector<EJBClientContext> setupEJBClientContextSelector() throws IOException {
        log.info("*** reading EJBClientContextSelector properties");
        // setup the selector
        final String clientPropertiesFile = "org/jboss/as/test/manualmode/ejb/ssl/jboss-ejb-client.properties";
        final InputStream inputStream = SSLEJBRemoteClientTestCase.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        final EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
        log.info("*** creating EJBClientContextSelector");
        final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);
        log.info("*** applying EJBClientContextSelector");
        return EJBClientContext.setSelector(selector);
    }


    @Before
    public void prepareServerOnce() throws Exception {
        if(!serverConfigDone) {
            // prepare server config and then restart
            log.info("*** preparing server configuration");
            ManagementClient managementClient;
            log.info("*** starting server");
            container.start(DEFAULT_JBOSSAS);
            final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
            managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
            log.info("*** will configure server now");
            SSLRealmSetupTool.setup(managementClient);
            log.info("*** restarting server");
            container.stop(DEFAULT_JBOSSAS);
            container.start(DEFAULT_JBOSSAS);
            managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
            // write SSL realm config to output - debugging purposes
            SSLRealmSetupTool.readSSLRealmConfig(managementClient);
            previousClientContextSelector = setupEJBClientContextSelector();
            serverConfigDone = true;
        } else {
            log.info("*** Server already prepared, skipping config procedure");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (previousClientContextSelector != null) {
            EJBClientContext.setSelector(previousClientContextSelector);
        }
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient mClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        SSLRealmSetupTool.tearDown(mClient, container);
    }

    private Properties getEjbClientContextProperties() {
        Properties env = new Properties();
        env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        env.put("jboss.naming.client.ejb.context", true);
        return env;
    }

    @Test
    public void testStatelessBean() throws Exception {
        log.info("**** deploying deployment with stateless beans");
        deployer.deploy(DEPLOYMENT_STATELESS);
        log.info("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties());
        try {
            log.info("**** looking up StatelessBean through JNDI");
            StatelessBeanRemote bean = (StatelessBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME_STATELESS + "/" + StatelessBean.class.getSimpleName() + "!" + StatelessBeanRemote.class.getCanonicalName());
            log.info("**** About to perform synchronous call on stateless bean");
            String response = bean.sayHello();
            log.info("**** The answer is: " + response);
            Assert.assertEquals("Remote invocation of EJB was not successful", StatelessBeanRemote.ANSWER, response);
            deployer.undeploy(DEPLOYMENT_STATELESS);
        } finally {
            ctx.close();
        }
    }

    @Test
    public void testStatelessBeanAsync() throws Exception {
        log.info("**** deploying deployment with stateless beans");
        deployer.deploy(DEPLOYMENT_STATELESS);
        log.info("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties());
        try {
            log.info("**** looking up StatelessBean through JNDI");
            StatelessBeanRemote bean = (StatelessBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME_STATELESS + "/" + StatelessBean.class.getSimpleName() + "!" + StatelessBeanRemote.class.getCanonicalName());
            log.info("**** About to perform asynchronous call on stateless bean");
            Future<String> futureResponse = bean.sayHelloAsync();
            String response = futureResponse.get();
            log.info("**** The answer is: " + response);
            Assert.assertEquals("Remote asynchronous invocation of EJB was not successful", StatelessBeanRemote.ANSWER, response);
            deployer.undeploy(DEPLOYMENT_STATELESS);
        } finally {
            ctx.close();
        }
    }

    @Test
    public void testStatefulBean() throws Exception {
        log.info("**** deploying deployment with stateful beans");
        deployer.deploy(DEPLOYMENT_STATEFUL);
        log.info("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties());
        try {
            log.info("**** looking up StatefulBean through JNDI");
            StatefulBeanRemote bean = (StatefulBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME_STATEFUL + "/" + StatefulBean.class.getSimpleName() + "!" + StatefulBeanRemote.class.getCanonicalName()+"?stateful");
            log.info("**** About to perform synchronous call on stateful bean");
            String response = bean.sayHello();
            log.info("**** The answer is: " + response);
            Assert.assertEquals("Remote invocation of EJB was not successful", StatefulBeanRemote.ANSWER, response);
            deployer.undeploy(DEPLOYMENT_STATEFUL);
        } finally {
            ctx.close();
        }
    }

    @Test
    public void testStatefulBeanAsync() throws Exception {
        log.info("**** deploying deployment with stateful beans");
        deployer.deploy(DEPLOYMENT_STATEFUL);
        log.info("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties());
        try {
            log.info("**** looking up StatefulBean through JNDI");
            StatefulBeanRemote bean = (StatefulBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME_STATEFUL + "/" + StatefulBean.class.getSimpleName() + "!" + StatefulBeanRemote.class.getCanonicalName()+"?stateful");
            log.info("**** About to perform asynchronous call on stateful bean");
            Future<String> futureResponse = bean.sayHelloAsync();
            String response = futureResponse.get();
            log.info("**** The answer is: " + response);
            Assert.assertEquals("Remote asynchronous invocation of EJB was not successful", StatefulBeanRemote.ANSWER, response);
            deployer.undeploy(DEPLOYMENT_STATEFUL);
        } finally {
            ctx.close();
        }
    }


}