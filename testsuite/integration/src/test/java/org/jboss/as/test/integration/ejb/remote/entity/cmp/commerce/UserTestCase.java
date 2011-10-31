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
package org.jboss.as.test.integration.ejb.remote.entity.cmp.commerce;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.remote.common.AnonymousCallbackHandler;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.remoting.IoFutureHelper;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;

@RunWith(Arquillian.class)
@RunAsClient
public class UserTestCase {
    private static final String APP_NAME = "cmp-commerce";
    private static final String MODULE_NAME = "ejb";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Connection connection;
    private EJBClientContext ejbClientContext;

    @Deployment
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(CommerceTestCase.class.getPackage());
        jar.addAsManifestResource("ejb/remote/entity/cmp/commerce/MANIFEST.MF", "MANIFEST.MF");
        jar.addAsManifestResource("ejb/remote/entity/cmp/commerce/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/remote/entity/cmp/commerce/jboss.xml", "jboss.xml");
        jar.addAsManifestResource("ejb/remote/entity/cmp/commerce/jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
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
        final IoFuture<Connection> futureConnection = endpoint.connect(new URI("remote://localhost:9999"), OptionMap.create(Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE), new AnonymousCallbackHandler());
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

    private UserHome getUserHome() {
        final EJBHomeLocator<UserHome> locator = new EJBHomeLocator<UserHome>(UserHome.class, APP_NAME, MODULE_NAME, "UserEJB", "");
        return EJBClient.createProxy(locator);
    }

    @Test
    public void testDeclaredSql() throws Exception {
        UserHome userHome = getUserHome();

        try {
            User main = userHome.create("main");
            User tody1 = userHome.create("tody1");
            User tody2 = userHome.create("tody2");
            User tody3 = userHome.create("tody3");
            User tody4 = userHome.create("tody4");

            Collection userIds = main.getUserIds();

            assertTrue(userIds.size() == 5);
            Iterator i = userIds.iterator();
            assertTrue(i.next().equals("main"));
            assertTrue(i.next().equals("tody1"));
            assertTrue(i.next().equals("tody2"));
            assertTrue(i.next().equals("tody3"));
            assertTrue(i.next().equals("tody4"));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error in testDeclaredSql");
        }

        deleteAllUsers(userHome);
    }


    public void deleteAllUsers(UserHome userHome) throws Exception {
        Iterator currentUsers = userHome.findAll().iterator();
        while (currentUsers.hasNext()) {
            User user = (User) currentUsers.next();
            user.remove();
        }
    }
}



