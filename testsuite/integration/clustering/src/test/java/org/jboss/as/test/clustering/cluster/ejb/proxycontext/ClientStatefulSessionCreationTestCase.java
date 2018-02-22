/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.proxycontext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.PropertyPermission;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.ejb.proxycontext.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.proxycontext.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.proxycontext.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.proxycontext.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.ejb.ClientEJBDirectory;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.ejb.client.NodeAffinity;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.function.ExceptionSupplier;

import javax.ejb.NoSuchEJBException;

/**
 * Validates stateful session creation across multiple contecxts (security, client context, etc)
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientStatefulSessionCreationTestCase extends ClusterAbstractTestCase {
    private static final String MODULE_NAME = "client-stateful-session-creation-test";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment(MODULE_NAME);
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment(MODULE_NAME);
    }

    static Archive<?> createDeployment(String moduleName) {
        return ShrinkWrap.create(JavaArchive.class, moduleName + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatefulIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    private final ExceptionSupplier<EJBDirectory, Exception> directoryProvider;
    private EJBClientContext previousContext;

    public ClientStatefulSessionCreationTestCase() {
        this.directoryProvider = () -> new ClientEJBDirectory(MODULE_NAME);
    }

    /**
     * Tests session creation with the following parameters:
     *
     * server environment: cluster "ejb" = {node-0, node-1}
     * EJBClientContext: connections = {node-0, node-1}
     *
     * Expected result: session created on node-0 or node-1
     *
     * @throws Exception
     */
    @Test
    public void testSessionCreationNoFailover() throws Exception {

        dumpConfiguredConnections("testSessionCreationNoFailover");

        try (EJBDirectory directory = this.directoryProvider.get()) {
            // create a session
            Incrementor bean = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);

            // validate the session proxy returned
            Assert.assertTrue("unexpected proxy affinity value",
                    validateStatefulSessionProxy(bean,new ClusterAffinity("ejb"), new NodeAffinity("node-0")) ||
                    validateStatefulSessionProxy(bean,new ClusterAffinity("ejb"), new NodeAffinity("node-1")));

        } catch(Exception e) {
            Assert.fail();
        }
    }

    /**
     * Tests session creation with the following parameters:
     *
     * server environment: cluster "ejb" = {node-1}
     * EJBClientContext: connections = {node-0}
     *
     * Expected result: session created on node-1
     *
     * @throws Exception
     */
    @Test
    public void testSessionCreationWithFailover() throws Exception {

        // remove the deployment from node-0
        undeploy(DEPLOYMENT_1);

        EJBClientContext testContext = createSingleConnectionContext("remote+http://127.0.0.1:8080");
        installCustomEJBClientContext(testContext);

        dumpConfiguredConnections("testSessionCreationWithFailover");

        try (EJBDirectory directory = this.directoryProvider.get()) {
            // create a session
            Incrementor bean = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);

            // validate the session proxy returned
            Assert.assertTrue("unexpected proxy affinity value",
                    validateStatefulSessionProxy(bean,new ClusterAffinity("ejb"), new NodeAffinity("node-1")));

        } catch(Exception e) {
            Assert.fail("Exception occurred creating session: e = " + e.getMessage());
        } finally {
           uninstallCustomEJBClientContext();
        }

        // redeploy onto node-0
        deploy(DEPLOYMENT_1);
    }

    /**
     * Tests session creation with the following parameters:
     *
     * server environment: cluster "ejb" = {node-1}
     * EJBClientContext: connections = {node-2} (non-existent!)
     *
     * Expected result: session creation results in NoSuchEJBException
     *
     * @throws Exception
     */
    @Test (expected = NoSuchEJBException.class)
    public void testSessionCreationWithNonExistantServer() throws Exception {

        EJBClientContext testContext = createSingleConnectionContext("remote+http://127.0.0.1:8280");
        installCustomEJBClientContext(testContext);

        dumpConfiguredConnections("testSessionCreationWithFailover");

        try (EJBDirectory directory = this.directoryProvider.get()) {
            // create a session
            Incrementor bean = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);

        } finally {
            uninstallCustomEJBClientContext();
        }
    }

    private void installCustomEJBClientContext(EJBClientContext newContext) {
        this.previousContext = EJBClientContext.getCurrent();
        EJBClientContext.getContextManager().setGlobalDefault(newContext);
    }

    private void uninstallCustomEJBClientContext() {
        EJBClientContext.getContextManager().setGlobalDefault(previousContext);
    }

    private void dumpConfiguredConnections(String testName) {
        System.out.println("Dumping configured connections for test: " + testName);
        for (EJBClientConnection conn : EJBClientContext.getCurrent().getConfiguredConnections()) {
            System.out.println("connection = " + conn.getDestination() + ", isForDiscovery = " + conn.isForDiscovery());
        }
    }
    /**
     * Create an EJBClientContext with one connection pointing to a URI string
     *
     * @return an EJBClientContext
     */
    private EJBClientContext createSingleConnectionContext(String connectionString) {
        EJBClientContext.Builder ctxBuilder = new EJBClientContext.Builder();
        // add a sngle connection to the context
        EJBClientConnection.Builder connBuilder = new EJBClientConnection.Builder();
        URI uri;
        try {
            uri = new URI(connectionString);
        } catch(URISyntaxException use) {
            uri = null;
            Assert.fail("Unexpected exception in test execution: e = " + use.getMessage());
        }
        connBuilder.setDestination(uri);
        connBuilder.setForDiscovery(true);
        ctxBuilder.addClientConnection(connBuilder.build());
        // need to add in transport providers from classpath for the context to build correctly
        ServiceLoader<EJBTransportProvider> serviceLoader = ServiceLoader.load(EJBTransportProvider.class, this.getClass().getClassLoader());
        Iterator<EJBTransportProvider> iterator = serviceLoader.iterator();
        for (;;) try {
            if (!iterator.hasNext()) break;
            final EJBTransportProvider transportProvider = iterator.next();
            ctxBuilder.addTransportProvider(transportProvider);
        } catch(ServiceConfigurationError sce) {
            Assert.fail("Failed to load service: exception = " + sce.getMessage()) ;
        }

        EJBClientContext result = ctxBuilder.build();
        return result;
    }

    /**
     * This method validates the returned proxy by checking its strong and weak affinity values.
     *
     * @param bean The proxy to be validated
     * @return true if the affinity settings are correct, false otherwise
     */
    private boolean validateStatefulSessionProxy(Incrementor bean, Affinity expectedStrongAffinity, Affinity expectedWeakAffinity) {
        Affinity actualStrongAffinity = EJBClient.getStrongAffinity(bean);
        Affinity actualWeakAffinity = EJBClient.getWeakAffinity(bean);
        return expectedStrongAffinity.equals(actualStrongAffinity) && expectedWeakAffinity.equals(actualWeakAffinity);
    }
}
