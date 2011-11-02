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

package org.jboss.as.test.integration.ejb.remote.entity.bmp;

import java.net.URI;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.RemoveException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.remote.common.AnonymousCallbackHandler;
import org.jboss.as.test.integration.ejb.remote.common.EJBRemoteManagementUtil;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Tests bean managed persistence
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BMPRemoteEntityBeanTestCase {

    private static final Logger logger = Logger.getLogger(BMPRemoteEntityBeanTestCase.class);

    private static final String APP_NAME = "ejb-remote-test";
    private static final String MODULE_NAME = "ejb";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Connection connection;
    private EJBClientContext ejbClientContext;

    @Deployment
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(BMPRemoteEntityBeanTestCase.class.getPackage());
        jar.addAsManifestResource("ejb/remote/entity/bmp/ejb-jar.xml", "ejb-jar.xml");
        ear.addAsModule(jar);
        return ear;
    }

    /**
     * Create and setup the remoting connection
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeTestClass() throws Exception {
        final Endpoint endpoint = Remoting.createEndpoint("endpoint", OptionMap.EMPTY);
        endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));

        // open a connection
        final int ejbRemotingPort = EJBRemoteManagementUtil.getEJBRemoteConnectorPort("localhost", 9999);
        final IoFuture<Connection> futureConnection = endpoint.connect(new URI("remote://localhost:" + ejbRemotingPort), OptionMap.create(Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE), new AnonymousCallbackHandler());
        connection = IoFutureHelper.get(futureConnection, 5, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void afterTestClass() throws Exception {
        executor.shutdown();
    }

    /**
     * Create and setup the EJB client context backed by the remoting receiver
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        this.ejbClientContext = EJBClientContext.create();
        this.ejbClientContext.registerConnection(connection);
    }

    @After
    public void afterTest() throws Exception {
        if (this.ejbClientContext != null) {
            EJBClientContext.suspendCurrent();
        }
    }

    @Test
    public void testSimpleCreate() throws Exception {
        final DataStore dataStore = getDataStore();
        dataStore.clear();
        final BMPHome home = getHome();
        final BMPInterface ejbInstance = home.createWithValue("Hello");
        logger.info(((EntityEJBLocator)EJBClient.getLocatorFor(ejbInstance)).getPrimaryKey());
        final Integer pk = (Integer) ejbInstance.getPrimaryKey();
        Assert.assertEquals("Hello", dataStore.get(pk));
    }

    @Test
    public void testFindByPrimaryKey() throws Exception {
        final DataStore dataStore = getDataStore();
        dataStore.clear();
        final BMPHome home = getHome();
        dataStore.put(1099, "VALUE1099");
        BMPInterface result = home.findByPrimaryKey(1099);
        Assert.assertEquals("VALUE1099", result.getMyField());
    }

    @Test
    public void testSingleResultFinderMethod() throws Exception {
        final DataStore dataStore = getDataStore();
        dataStore.clear();
        final BMPHome home = getHome();
        dataStore.put(888, "VALUE888");
        BMPInterface result = home.findByValue("VALUE888");
        Assert.assertEquals("VALUE888", result.getMyField());
        Assert.assertEquals(888, result.getPrimaryKey());
    }


    @Test
    public void testCollectionFinderMethod() throws Exception {
        final DataStore dataStore = getDataStore();
        dataStore.clear();
        final BMPHome home = getHome();
        dataStore.put(1000, "Collection");
        dataStore.put(1001, "Collection");
        Collection<BMPInterface> col = home.findCollection();
        for (BMPInterface result : col) {
            Assert.assertEquals("Collection", result.getMyField());
        }
    }

    @Test
    public void testRemoveEntityBean() throws Exception {
        final DataStore dataStore = getDataStore();
        dataStore.clear();
        final BMPHome home = getHome();
        dataStore.put(56, "Remove");
        BMPInterface result = home.findByPrimaryKey(56);
        Assert.assertEquals("Remove", result.getMyField());
        result.remove();
        Assert.assertFalse(dataStore.containsKey(56));
        try {
            result.getMyField();
            fail("Expected invocation on removed instance to fail");
        } catch (NoSuchObjectException expected) {

        }
    }

    @Test
    public void testIsIdentical() throws Exception {
        final DataStore dataStore = getDataStore();
        dataStore.clear();
        final BMPHome home = getHome();
        dataStore.put(40, "1");
        dataStore.put(41, "2");
        BMPInterface bean1 = home.findByPrimaryKey(40);
        BMPInterface bean1_2 = home.findByPrimaryKey(40);
        BMPInterface bean2 = home.findByPrimaryKey(41);
        Assert.assertTrue(bean1.isIdentical(bean1_2));
        Assert.assertFalse(bean1.isIdentical(bean2));
    }

    @Test
    public void testEjbHomeMethod() throws Exception {
        final BMPHome home = getHome();
        Assert.assertEquals(SimpleBMPBean.HOME_METHOD_RETURN, home.exampleHomeMethod());
    }

    @Test
    public void testGetEJBLocalHome() throws Exception {
        final DataStore dataStore = getDataStore();
        dataStore.clear();
        final BMPHome home = getHome();
        dataStore.put(23, "23");
        BMPInterface result = home.findByPrimaryKey(23);
        final BMPHome home2 = (BMPHome) result.getEJBHome();
        Assert.assertEquals(SimpleBMPBean.HOME_METHOD_RETURN, home2.exampleHomeMethod());
    }

    @Test
    public void testHomeInterfaceEquality() throws Exception {
        final BMPHome home1 = getHome();
        final BMPHome home2 = getHome();
        Assert.assertEquals(home1, home2);
        Assert.assertEquals(home1.hashCode(), home2.hashCode());
        Assert.assertNotSame(home1, new BMPHome() {
            public BMPInterface createEmpty() {
                return null;
            }

            public BMPInterface createWithValue(String value) {
                return null;
            }

            public BMPInterface findByPrimaryKey(Integer primaryKey) {
                return null;
            }

            public BMPInterface findByValue(String value) {
                return null;
            }

            public Collection<BMPInterface> findCollection() {
                return null;
            }

            public int exampleHomeMethod() {
                return 0;
            }

            public void remove(Handle handle) throws RemoteException, RemoveException {

            }

            public void remove(Object primaryKey) throws RemoteException, RemoveException {

            }

            public EJBMetaData getEJBMetaData() throws RemoteException {
                return null;
            }

            public HomeHandle getHomeHandle() throws RemoteException {
                return null;
            }
        });
    }

    private BMPHome getHome() {
        final EJBHomeLocator<BMPHome> locator = new EJBHomeLocator<BMPHome>(BMPHome.class, APP_NAME, MODULE_NAME, "SimpleBMP", "");
        return EJBClient.createProxy(locator);
    }

    private DataStore getDataStore() {
        final StatelessEJBLocator<DataStore> locator = new StatelessEJBLocator<DataStore>(DataStore.class, APP_NAME, MODULE_NAME, DataStoreBean.class.getSimpleName(), "");
        return EJBClient.createProxy(locator);
    }
}
