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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.ejb.EJBHome;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81036 $</tt>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LazyResultSetLoadingTestCase {

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

    private OrderHome getOrderHome() {
        return getHome(OrderHome.class, "OrderEJB");
    }

    private LineItemHome getLineItemHome() {
        return getHome(LineItemHome.class, "LineItemEJB");
    }

    private <T extends EJBHome> T getHome(final Class<T> homeClass, final String beanName) {
        final EJBHomeLocator<T> locator = new EJBHomeLocator<T>(homeClass, APP_NAME, MODULE_NAME, beanName, "");
        return EJBClient.createProxy(locator);
    }

    @Test
    public void testLazyResultSetLoading() throws Exception {
        OrderHome oh = getOrderHome();
        Order o = oh.create(new Long(1));

        LineItemHome lih = getLineItemHome();
        LineItem li = lih.create(new Long(11));
        o.addLineItemId(li.getId());

        li = lih.create(new Long(22));
        o.addLineItemId(li.getId());

        li = lih.create(new Long(33));
        o.addLineItemId(li.getId());

        // empty result
        Collection col = oh.selectLazy("select object(o) from Address o where o.state='CA'", null);
        assertTrue("Expected empty collection but got " + col.size(), col.isEmpty());

        // collection of results
        col = oh.selectLazy("select object(o) from LineItem o", null);
        assertTrue("Expected 3 line items but got " + col.size(), 3 == col.size());

        Iterator i = col.iterator();
        LineItem removed = (LineItem) i.next();
        i.remove();
        assertTrue("Expected 2 line items but got " + col.size(), 2 == col.size());

        Collection firstPassCol = new ArrayList(2);
        while (i.hasNext()) {
            firstPassCol.add(i.next());
        }

        Collection secondPassCol = new ArrayList(3);
        i = col.iterator();
        while (i.hasNext()) {
            li = (LineItem) i.next();
            assertTrue(firstPassCol.contains(li));
            secondPassCol.add(li);
        }
        assertTrue("Expected 2 line items but got " + secondPassCol.size(), secondPassCol.size() == 2);
        secondPassCol.add(removed);
        assertTrue("Expected 3 line items but got " + secondPassCol.size(), secondPassCol.size() == 3);

        // limit & offset
        col = oh.selectLazy("select object(o) from LineItem o offset 1 limit 2", null);
        assertTrue("Expected 2 line items but got " + col.size(), col.size() == 2);
        int count = 0;
        for (i = col.iterator(); i.hasNext(); ) {
            i.next();
            ++count;
        }
        assertTrue("Expected 2 but got " + count, count == 2);
        oh.remove(new Long(1));
    }
}
