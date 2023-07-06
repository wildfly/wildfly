/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.net.URL;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.PropertyPermission;

import javax.naming.Context;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.servlet.WhichNodeServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.transaction.client.AbstractTransaction;
import org.wildfly.transaction.client.ContextTransactionManager;

import org.wildfly.common.function.ExceptionSupplier;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.management.util.ModelUtil.modelNodeAsStingList;

/**
 * A test case for the key features of EJB/HTTP when used in conjunction with a load balancer,
 * namely:
 *   - stickiness for stateful session beans
 *   - load balancing for stateless session beans
 *
 * The test servers are configured as follows:
 *   - load balancer (load-balancer-1) @ localhost:8580
 *   - backend server (node-1) @ localhost:8080
 *   - backend server (node-2) @ localhost:8180
 *
 * @author Richard Achmatowicz
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EJBOverHTTPTestCase.ServerSetupTask.class)
public class EJBOverHTTPTestCase extends AbstractClusteringTestCase {

    private static final int COUNT = 4;
    private static final String MODULE_NAME = EJBOverHTTPTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".jar";
    public static final long STATUS_REFRESH_TIMEOUT = 30_000;
    public static final int LB_OFFSET = 500;

    private static final PathAddress UNDERTOW = PathAddress.pathAddress().append(SUBSYSTEM,"undertow");
    private static final PathAddress REQUEST_LOGGING_FILTER = UNDERTOW.append("configuration","filter").append("custom-filter","request-logging-filter");
    private static final PathAddress REQUEST_LOGGING_FILETR_REF = UNDERTOW.append("server","default-server").append("host","default-host").append("filter-ref","request-logging-filter");
    private static final PathAddress DEFAULT_HTTP_LISTENER = UNDERTOW.append("server","default-server").append("http-listener","default");
    private static final PathAddress LOAD_BALANCER = UNDERTOW.append("configuration","filter").append("mod-cluster","load-balancer");
    private static final PathAddress RUNTIME_LOAD_BALANCER = LOAD_BALANCER.append("balancer","mycluster");

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createLoadBalancerCheck1() {
        return createLoadBalancerCheckDeployment();
    }

    @Deployment(name = DEPLOYMENT_4, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createLoadBalancerCheck2() {
        return createLoadBalancerCheckDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME)
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatefulIncrementorBean.class, StatelessIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    private static Archive<?> createLoadBalancerCheckDeployment() {
        return ShrinkWrap.create(WebArchive.class, "LoadBalancerCheck" + ".war")
                .addClasses(WhichNodeServlet.class)
                .setWebXML(WhichNodeServlet.class.getPackage(), "web.xml");
    }

    public EJBOverHTTPTestCase() {
        super(new String[] { NODE_1, NODE_2, LOAD_BALANCER_1 }, new String[]{ DEPLOYMENT_1, DEPLOYMENT_2, DEPLOYMENT_3, DEPLOYMENT_4 });
    }

    @Before
    public void beforeTest() throws Exception {
        log.infof(MODULE_NAME+ " : running before test ");
        installRequestDumperIntoLoadBalancer();
    }

    @After
    public void afterTest() throws Exception {
        log.infof(MODULE_NAME+ " : running after test ");
        removeRequestDumperFromLoadBalancer();
    }

    /*
     * Check to see that the load balancer, so configured, will balance HTTP servlet requests correctly.
     */
    @InSequence(1)
    @Test
    public void testLoadBalancer() throws Exception {
        log.infof(MODULE_NAME+ " : testLoadBalancer: starting test");
        waitForProxyRegistration();

        URI uri = WhichNodeServlet.createURI(new URL("http", "localhost", 8580, "/LoadBalancerCheck/"));
        log.infof("Sending invovation to %s", uri.toString());

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                log.infof("first HTTP request went to %s", response.getFirstHeader("nodename").getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                log.infof("second HTTP request went to %s", response.getFirstHeader("nodename").getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    @InSequence(2)
    @Test
    public void testSLSBWithoutFailover() throws Exception {
        waitForProxyRegistration();

        // check SLSB load balance
        testSLSBWithoutFailover(() -> new RemoteEJBDirectory(MODULE_NAME, getProperties(false)));
    }

    @InSequence(3)
    @Test
    public void testSFSBWithoutFailover() throws Exception {
        waitForProxyRegistration();

        // check SFSB failover for a single SFSB
        testSFSBWithoutFailover(() -> new RemoteEJBDirectory(MODULE_NAME, getProperties(false)));
    }

    @InSequence(4)
    @Test
    public void testSFSBAndSLSBWithoutFailover() throws Exception {
        waitForProxyRegistration();

        // check SFSB and SLSB do not interfere
        testSFSBAndSLSBWithoutFailover(() -> new RemoteEJBDirectory(MODULE_NAME, getProperties(false)));
    }

    @InSequence(5)
    @Test
    public void testMultipleSFSBWithoutFailover() throws Exception {
        waitForProxyRegistration();

        // check multiple SFSB sessions are created on different nodes
        testMultipleSFSBWithoutFailover(() -> new RemoteEJBDirectory(MODULE_NAME, getProperties(false)));
    }

    @Ignore
    @InSequence(6)
    @Test
    public void testSFSBInTransactionWithoutFailover() throws Exception {
        waitForProxyRegistration();

        // check SFSB failover for a single SFSB
        testSFSBInTransactionWithoutFailover(() -> new RemoteEJBDirectory(MODULE_NAME, getProperties(false)));
    }

    /*
     * A test which checks SLSB behaviour of EJB client with EJB/HTTP in the absence of failover.
     * The key behaviour to validate: load balancing.
     * Proxies are obtained via JNDI/HTTP and invocations are made using EJB/HTTP.
     */
    public void testSLSBWithoutFailover(ExceptionSupplier<EJBDirectory, Exception> directoryProvider) throws Exception {

        log.infof(MODULE_NAME+ " : testSLSBWithoutFailover: starting test");
        try (EJBDirectory directory = directoryProvider.get()) {
            Incrementor slsb = directory.lookupStateless(StatelessIncrementorBean.class, Incrementor.class);
            logAffinityForBean(slsb, "testSLSBWithoutFailover");

            Result<Integer> result = slsb.increment();
            log.info("Called SLSBWithoutFailover: SLSB backend node = " + result.getNode());

            int count = 1;
            for (int i = 0; i < COUNT; ++i) {
                result = slsb.increment();
                log.info("Called SLSBWithoutFailover: SLSB backend node = " + result.getNode());
            }
        }
    }

    /*
     * A test which checks SFSB behaviour of EJB client with EJB/HTTP in the absence of failover.
     * The key behaviour to validate: stickiness of EJB sessions to the nodes that own them.
     * Proxies are obtained via JNDI/HTTP and invocations are made using EJB/HTTP.
     */
    public void testSFSBWithoutFailover(ExceptionSupplier<EJBDirectory, Exception> directoryProvider) throws Exception {

        log.infof(MODULE_NAME+ " : testSFSBWithoutFailover: starting test");
        try (EJBDirectory directory = directoryProvider.get()) {
            // this single statement creates a session on the server
            Incrementor sfsb = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);
            logAffinityForBean(sfsb, "testSFSBWithoutFailover");

            Result<Integer> result = sfsb.increment();
            log.infof("Called SFSBWithoutFailover: SFSB value = %s, backend node = %s", result.getValue().intValue(), result.getNode());

            int count = 1;
            for (int i = 0; i < COUNT; ++i) {
                result = sfsb.increment();
                log.infof("Called SFSBWithoutFailover: SFSB value = %s, backend node = %s", result.getValue().intValue(), result.getNode());
            }
        }
    }

    /*
     * A test which checks SFSB and SLSB behaviour of EJB client with EJB/HTTP in the absence of failover.
     * The key behaviour to validate: stickiness of EJB SFSB sessions to the nodes that own them, while at the
     * same time allowing SLSB EJBs to balance across nodes.
     * Proxies are obtained via JNDI/HTTP and invocations are made using EJB/HTTP.
     */
    public void testSFSBAndSLSBWithoutFailover(ExceptionSupplier<EJBDirectory, Exception> directoryProvider) throws Exception {

        log.infof(MODULE_NAME+ " : testSLSBWAndSLSBithoutFailover: starting test");
        try (EJBDirectory directory = directoryProvider.get()) {
            // this single statement creates a session on the server
            Incrementor sfsb = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);
            Incrementor slsb = directory.lookupStateless(StatelessIncrementorBean.class, Incrementor.class);

            Result<Integer> sfsbResult = sfsb.increment();
            log.infof("Called SFSBAndSLSBWithoutFailover: SFSB value = %s, backend node = %s", sfsbResult.getValue().intValue(), sfsbResult.getNode());

            Result<Integer> slsbResult = slsb.increment();
            log.infof("Called SFSBAndSLSBWithoutFailover: SLSB backend node = %s", slsbResult.getNode());

            int count = 1;
            for (int i = 0; i < COUNT; ++i) {
                sfsbResult = sfsb.increment();
                log.infof("Called SFSBAndSLSBWithoutFailover: SFSB value = %s, backend node = %s", sfsbResult.getValue().intValue(), sfsbResult.getNode());

                slsbResult = slsb.increment();
                log.infof("Called SFSBAndSLSBWithoutFailover: SLSB backend node = %s", slsbResult.getNode());
            }
        }
    }

    /*
     * A test which checks SFSB behaviour of EJB client with EJB/HTTP in the absence of failover.
     * The key behaviour to validate: multiple SFSB sessions are distributed evenly across the cluster
     * Proxies are obtained via JNDI/HTTP and invocations are made using EJB/HTTP.
     */
    public void testMultipleSFSBWithoutFailover(ExceptionSupplier<EJBDirectory, Exception> directoryProvider) throws Exception {

        log.infof(MODULE_NAME+ " : testMultipleSFSBWithoutFailover: starting test");
        try (EJBDirectory directory = directoryProvider.get()) {
            // this single statement creates a session on the server
            Incrementor sfsb1 = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);
            Incrementor sfsb2 = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);
            logAffinityForBean(sfsb1, "testSFSBWithoutFailover");
            logAffinityForBean(sfsb2, "testSFSBWithoutFailover");

            Result<Integer> result1 = sfsb1.increment();
            log.infof("Called MultipleSFSBWithoutFailover: SFSB value = %s, backend node = %s", result1.getValue().intValue(), result1.getNode());

            Result<Integer> result2 = sfsb2.increment();
            log.infof("Called MultipleSFSBWithoutFailover: SFSB value = %s, backend node = %s", result2.getValue().intValue(), result2.getNode());

            int count = 1;
            for (int i = 0; i < COUNT; ++i) {
                result1 = sfsb1.increment();
                log.infof("Called MultipleSFSBWithoutFailover: SFSB value = %s, backend node = %s", result1.getValue().intValue(), result1.getNode());

                result2 = sfsb2.increment();
                log.infof("Called MultipleSFSBWithoutFailover: SFSB value = %s, backend node = %s", result2.getValue().intValue(), result2.getNode());
            }
        }
    }

    /*
     * A test which checks SFSB behaviour of EJB client with EJB/HTTP in the absence of failover.
     * The key behaviour to validate: stickiness of EJB sessions to the nodes that own them.
     * Proxies are obtained via JNDI/HTTP and invocations are made using EJB/HTTP.
     */
    public void testSFSBInTransactionWithoutFailover(ExceptionSupplier<EJBDirectory, Exception> directoryProvider) throws Exception {

        log.infof(MODULE_NAME+ " : testSFSBInTransactionWithoutFailover: starting test");
        try (EJBDirectory directory = directoryProvider.get()) {

            UserTransaction tx = EJBClient.getUserTransaction("dummy");
            tx.begin();
            log.infof("begin txn: type = %s, status = %s, thread = %s", tx.getClass().getName(), tx.getStatus(), Thread.currentThread().toString());
            ContextTransactionManager tm = ContextTransactionManager.getInstance();
            AbstractTransaction txn = tm.getTransaction();
            log.infof("check txn: type = %s, status = %s, thread = %s", txn.getClass().getName(), txn.getStatus(), Thread.currentThread().toString());



            // this single statement creates a session on the server
            Incrementor sfsb = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);
            logAffinityForBean(sfsb, "testSFSBInTransactionWithoutFailover");

            Result<Integer> result = sfsb.increment();
            log.infof("Called SFSBInTransactionWithoutFailover: SFSB value = %s, backend node = %s", result.getValue().intValue(), result.getNode());

            int count = 1;
            for (int i = 0; i < COUNT; ++i) {
                result = sfsb.increment();
                log.infof("Called SFSBInTransactionWithoutFailover: SFSB value = %s, backend node = %s", result.getValue().intValue(), result.getNode());
            }

            tx.commit();
        }
    }


    private void logAffinityForBean(Object bean, String message) {
        Affinity strongAffinity = EJBClient.getStrongAffinity(bean);
        Affinity weakAffinity = EJBClient.getWeakAffinity(bean);
        log.infof("%s: SFSB affinity for bean %s, strong affinity = %s, weak affinity = %s", message, bean, strongAffinity, weakAffinity);
    }

    /*
     * Set up JNDI properties to support HTTP based Jakarta Enterprise Beans client invocations via EJB/HTTP
     *
     * NOTE: there are several ways to connect to the Jakarta Enterprise Beans container on the server:
     *   protocol           URL
     *   remoting           remote://localhost:4447
     *   HTTP Upgrade       remote+http://localhost:8080
     *   pure HTTP          http://localhost:8080/wildfly-sevices
     */
    private static Properties getProperties(boolean isSecure) {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, org.wildfly.naming.client.WildFlyInitialContextFactory.class.getName());
        if (isSecure) {
            props.put(Context.PROVIDER_URL, String.format("%s://%s:%s/wildfly-services", "https", "localhost", "8580"));
            props.put(Context.SECURITY_PRINCIPAL, "remoteejbuser");
            props.put(Context.SECURITY_CREDENTIALS, "rem@teejbpasswd1");
        } else {
            props.put(Context.PROVIDER_URL, String.format("%s://%s:%s/wildfly-services", "http", "localhost", "8580"));
            props.put(Context.SECURITY_PRINCIPAL, "remoteejbuser");
            props.put(Context.SECURITY_CREDENTIALS, "rem@teejbpasswd1");
        }
        return props ;
    }

    /*
     * Periodically check rhe load balancer for registered backend servers.
     * Return when both expected backend servers are registered.
     */
    private void waitForProxyRegistration() throws Exception {
        final ModelControllerClient client = getModelControllerClient();

        // /subsystem=undertow/configuration=filter/mod-cluster=load-balancer/balancer=mycluster:read-resource(include-runtime)
        ModelNode readRegisteredWorkersOperation = Util.createOperation(READ_CHILDREN_NAMES_OPERATION, RUNTIME_LOAD_BALANCER);
        readRegisteredWorkersOperation.get(CHILD_TYPE).set("node");
        readRegisteredWorkersOperation.get(RECURSIVE).set("true");
        readRegisteredWorkersOperation.get(INCLUDE_RUNTIME).set("true");

        log.info("Waiting for backend server registration to complete");
        // wait until we see two worker nodes on the load balancer
        long start = System.currentTimeMillis();
        // the result is either OUTCOME="failed" + FAILURE_DESCRIPTION or OUTCOME="success" + RESULT
        ModelNode result = null;
        boolean allWorkersRegistered = false;
        while (System.currentTimeMillis() - start < STATUS_REFRESH_TIMEOUT) {
            result = client.execute(readRegisteredWorkersOperation);
            if (result.get(OUTCOME).asString().equals(SUCCESS)) {
               // log.info("result = " + result.toString());
                List<String> registeredWorkersList = modelNodeAsStingList(result.get(RESULT));
                if (registeredWorkersList.size() == 2) {
                    allWorkersRegistered = true;
                    log.info("registeredWorkersList = " + registeredWorkersList);
                    break;
                }
            }
            Thread.sleep(1000);
        }

        if (!allWorkersRegistered) {
            throw new RuntimeException("Test cannot proceed as all backend servers were not registered with the load balancer");
        }
    }

    /*
     * Install a request dumper into Undertow to see which requests are arriving at the load balancer     *
     */
    private void installRequestDumperIntoLoadBalancer() throws Exception {
        final ModelControllerClient client = getModelControllerClient();

        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();
        ModelNode steps = compositeOp.get(STEPS);

        // /subsystem=undertow/configuration=filter/custom-filter=request-logging-filter:add(class-name=io.undertow.server.handlers.RequestDumpingHandler,module=io.undertow.core)
        ModelNode addLoggingFilterModelNode = Util.createAddOperation(REQUEST_LOGGING_FILTER);
        addLoggingFilterModelNode.get("class-name").set("io.undertow.server.handlers.RequestDumpingHandler");
        addLoggingFilterModelNode.get("module").set("io.undertow.core");
        steps.add(addLoggingFilterModelNode);

        // /subsystem=undertow/server=default-server/host=default-host/filter-ref=request-logging-filter:add()
        ModelNode addLoggingFilterRefModelNode = Util.createAddOperation(REQUEST_LOGGING_FILETR_REF);
        steps.add(addLoggingFilterRefModelNode);

        // disable http2 on the defauult connector
        ModelNode disableHttp2DefaultListenerModelNode = Util.getWriteAttributeOperation(DEFAULT_HTTP_LISTENER, "enable-http2", false);
        steps.add(disableHttp2DefaultListenerModelNode);

        // disable http2 on load-balancer
        ModelNode disableHttp2LoadBalancerModelNode = Util.getWriteAttributeOperation(LOAD_BALANCER, "enable-http2", false);
        steps.add(disableHttp2LoadBalancerModelNode);

        Utils.applyUpdates(Collections.singletonList(compositeOp), client);
        ServerReload.reloadIfRequired(client);
    }

    /*
     * Install a request dumper into Undertow to see which requests are arriving at the load balancer
     */
    private void removeRequestDumperFromLoadBalancer() throws Exception {
        final ModelControllerClient client = getModelControllerClient();

        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();
        ModelNode steps = compositeOp.get(STEPS);

        // /subsystem=undertow/configuration=filter/custom-filter=request-logging-filter:remove()
        ModelNode removeLoggingFilterModelNode = Util.createRemoveOperation(REQUEST_LOGGING_FILTER);
        steps.add(removeLoggingFilterModelNode);

        // /subsystem=undertow/server=default-server/host=default-host/filter-ref=request-logging-filter:remove()
        ModelNode removeLoggingFilterRefModelNode = Util.createRemoveOperation(REQUEST_LOGGING_FILETR_REF);
        steps.add(removeLoggingFilterRefModelNode);

        // enable http2 on the defauult connector
        ModelNode enableHttp2DefaultListenerModelNode = Util.getWriteAttributeOperation(DEFAULT_HTTP_LISTENER, "enable-http2", true);
        steps.add(enableHttp2DefaultListenerModelNode);

        // enable http2 on the load balancer
        ModelNode enableHttp2LoadBalancerModelNode = Util.getWriteAttributeOperation(LOAD_BALANCER, "enable-http2", true);
        steps.add(enableHttp2LoadBalancerModelNode);

        Utils.applyUpdates(Collections.singletonList(compositeOp), client);
        ServerReload.reloadIfRequired(client);
    }

    /*
     * Get a management client to the load balancer for performing management operations.
     */
    private ModelControllerClient getModelControllerClient() {
        final String address = TestSuiteEnvironment.getServerAddress();
        final int port = TestSuiteEnvironment.getServerPort();
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient(null, address, port + LB_OFFSET);
        return client;
    }
    /*
     * This server setup task registers each of the servers with the load balancer.
     */
    static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder
                // configure one backend server for mod_cluster registration and Undertow request dumping
               .node(NODE_1)
                // configure for mod_cluster
               .setup("/subsystem=modcluster/proxy=default:write-attribute(name=advertise,value=false)")
               .setup("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy1:add(host=localhost,port=8590)")
               .setup("/subsystem=modcluster/proxy=default:list-add(name=proxies,value=proxy1)")
                // configure for Undertow request dumping
               .setup("/subsystem=undertow/configuration=filter/custom-filter=request-logging-filter:add(class-name=io.undertow.server.handlers.RequestDumpingHandler,module=io.undertow.core")
               .setup("/subsystem=undertow/server=default-server/host=default-host/filter-ref=request-logging-filter:add")
                // set up server side logging
               .setup("/subsystem=logging/logger=org.wildfly.extension.undertow:add()")
               .setup("/subsystem=logging/logger=org.wildfly.extension.undertow:write-attribute(name=level, value=TRACE)")
               .setup("/subsystem=logging/logger=org.wildfly.httpclient.ejb:add()")
               .setup("/subsystem=logging/logger=org.wildfly.httpclient.ejb:write-attribute(name=level, value=TRACE)")

               .teardown("/subsystem=logging/logger=org.wildfly.httpclient.ejb:remove()")
               .teardown("/subsystem=logging/logger=org.wildfly.extension.undertow:remove()")

               .teardown("/subsystem=modcluster/proxy=default:list-remove(name=proxies,value=proxy1)")
               .teardown("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy1:remove()")
               .teardown("/subsystem=modcluster/proxy=default:write-attribute(name=advertise,value=true)")

               .teardown("/subsystem=undertow/server=default-server/host=default-host/filter-ref=request-logging-filter:remove")
               .teardown("/subsystem=undertow/configuration=filter/custom-filter=request-logging-filter:remove")
               .parent()

               .node(NODE_2)
                // configure a second backend server
               .setup("/subsystem=modcluster/proxy=default:write-attribute(name=advertise,value=false)")
               .setup("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy2:add(host=localhost,port=8590)")
               .setup("/subsystem=modcluster/proxy=default:list-add(name=proxies,value=proxy2)")
                // configure for Undertow request dumping
               .setup("/subsystem=undertow/configuration=filter/custom-filter=request-logging-filter:add(class-name=io.undertow.server.handlers.RequestDumpingHandler,module=io.undertow.core")
               .setup("/subsystem=undertow/server=default-server/host=default-host/filter-ref=request-logging-filter:add")
               // set up server side logging (nly appears in server logs!)
               .setup("/subsystem=logging/logger=org.wildfly.extension.undertow:add()")
               .setup("/subsystem=logging/logger=org.wildfly.extension.undertow:write-attribute(name=level, value=TRACE)")
               .setup("/subsystem=logging/logger=org.wildfly.httpclient.ejb:add()")
               .setup("/subsystem=logging/logger=org.wildfly.httpclient.ejb:write-attribute(name=level, value=TRACE)")

               .teardown("/subsystem=logging/logger=org.wildfly.httpclient.ejb:remove()")
               .teardown("/subsystem=logging/logger=org.wildfly.extension.undertow:remove()")

               .teardown("/subsystem=modcluster/proxy=default:list-remove(name=proxies,value=proxy2)")
               .teardown("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy2:remove()")
               .teardown("/subsystem=modcluster/proxy=default:write-attribute(name=advertise,value=true)")

               .teardown("/subsystem=undertow/server=default-server/host=default-host/filter-ref=request-logging-filter:remove")
               .teardown("/subsystem=undertow/configuration=filter/custom-filter=request-logging-filter:remove")

                /*

                TODO: fix the server setup mechanism so we can operate on nodes which do not have deployments
               .node(LOAD_BALANCER_1)
                // configure request dumping on LB and access logging
                // request dumping
               .setup("/subsystem=undertow/configuration=filter/custom-filter=request-logging-filter:add(class-name=io.undertow.server.handlers.RequestDumpingHandler,module=io.undertow.core")
               .setup("/subsystem=undertow/server=default-server/host=default-host/filter-ref=request-logging-filter:add")
                // access log
               .setup("/subsystem=undertow/server=default-server/host=default-host/setting=access-log:add(pattern=\"%h %t \"%r\" %s \"%{i,User-Agent}\"\", use-server-log=true)")
               .teardown("/subsystem=undertow/server=default-server/host=default-host/setting=access-log:remove")
               .teardown("/subsystem=undertow/server=default-server/host=default-host/filter-ref=request-logging-filter:remove")
               .teardown("/subsystem=undertow/configuration=filter/custom-filter=request-logging-filter:remove")

                 */
            ;
        }
    }
}
