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

package org.jboss.as.test.integration.ejb.remote.client.api.tx;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.remote.common.AnonymousCallbackHandler;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientTransactionContext;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;

import javax.transaction.UserTransaction;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * User: jpai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBClientAPITransactionUsageTestCase {
    private static final Logger logger = Logger.getLogger(EJBClientAPITransactionUsageTestCase.class);

    private static Connection connection;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String APP_NAME = "ejb-remote-client-api-tx-test";

    private static final String MODULE_NAME = "ejb";

    private EJBClientContext ejbClientContext;

    /**
     * Creates an EJB deployment
     *
     * @return
     */
    @Deployment
    public static Archive<?> createDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(EJBClientAPITransactionUsageTestCase.class.getPackage());
        jar.addClass(AnonymousCallbackHandler.class);

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

    @Test
    public void testRemoteUserTransactionOnSLSB() throws Exception {
        final EJBClientTransactionContext ejbClientTransactionContext = EJBClientTransactionContext.createLocal();
        final UserTransaction userTransaction = EJBClient.getUserTransaction("dummynodename");
        userTransaction.begin();
        final StatelessEJBLocator<CMTRemote> cmtRemoteBeanLocator = new StatelessEJBLocator<CMTRemote>(CMTRemote.class, APP_NAME, MODULE_NAME, CMTBean.class.getSimpleName(), "");
        final CMTRemote cmtRemoteBean = EJBClient.createProxy(cmtRemoteBeanLocator);
        cmtRemoteBean.mandatoryTxOp();
        userTransaction.commit();
    }
}
