package org.jboss.as.test.clustering.cluster.ejb.remote;


import org.apache.commons.lang.math.RandomUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Heartbeat;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.HeartbeatBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.ejb.client.ClusterNodeSelector;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.protocol.remote.RemoteTransportProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.PropertyPermission;

/**
 * <p>Validates that the provided implementation of {@link ClusterNodeSelector} is being used by the ejb client to
 * select the node of the cluster to route the request to;</p>
 * <p>This relies on the cluster provisioning the client with a cluster view containing the correct affinity for the
 * EJB being invoked;</p>
 * <p>This test basically replicates test org.jboss.ejb.client.test.ClusterNodeSelectorTestCase in project
 * jboss-ejb-client (credits to <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>);</p>
 * <p>This test was added after the functionality was broken because of the affinity not being correctly sent to the
 * client (refer to issue <a href="https://issues.jboss.org/browse/WFLY-10030">WFLY-10030</a>);</p>
 * <p>NOTE: this test can be run using the following command issued from the root directory of the whole WildFly project:
 * {@code ./integration-tests.sh test -Dts.noSmoke -Dts.clustering -Dtest=org.jboss.as.test.clustering.cluster.ejb.remote.ClientClusterNodeSelectorTestCase}</p>
 *
 * @author <a href="mailto:tborgato@redhat.com">Tommaso Borgato</a>
 */
@RunWith(Arquillian.class)
public class ClientClusterNodeSelectorTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = ClientClusterNodeSelectorTestCase.class.getSimpleName();
    private static final long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(2000);
    /**
     * Implementation of {@link ClusterNodeSelector} to be used for custom cluster node selection
     */
    public static class CustomClusterNodeSelector implements ClusterNodeSelector {
        private static volatile String PICK_NODE = null;

        @Override
        public String selectNode(
                String clusterName, String[] connectedNodes, String[] totalAvailableNodes) {
            if (PICK_NODE != null) {
                return PICK_NODE;
            }
            return connectedNodes[0];
        }
    }

    public ClientClusterNodeSelectorTestCase() throws Exception {
        super(FOUR_NODES);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment(MODULE_NAME);
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment(MODULE_NAME);
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_3)
    public static Archive<?> createDeploymentForContainer3() {
        return createDeployment(MODULE_NAME);
    }

    @Deployment(name = DEPLOYMENT_4, managed = false, testable = false)
    @TargetsContainer(NODE_4)
    public static Archive<?> createDeploymentForContainer4() {
        return createDeployment(MODULE_NAME);
    }

    /**
     * Instructs the ClusterNodeSelector on which node to call before each request and that checks that the response
     * comes from that very node;
     */
    @Test
    public void test(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1
    ) throws Exception {
        EJBClientContext BKP = null;
        try {
            final EJBClientContext.Builder ejbClientBuilder = new EJBClientContext.Builder();
            ejbClientBuilder.addTransportProvider(new RemoteTransportProvider());
            final EJBClientConnection.Builder connBuilder = new EJBClientConnection.Builder();
            connBuilder.setDestination(URI.create(String.format("remote+%s", baseURL1)));
            ejbClientBuilder.addClientConnection(connBuilder.build());
            // ====================================================================================
            // set the custom ClusterNodeSelector
            // ====================================================================================
            ejbClientBuilder.setClusterNodeSelector(new CustomClusterNodeSelector());
            BKP = EJBClientContext.getContextManager().getThreadDefault();
            final EJBClientContext ejbCtx = ejbClientBuilder.build();
            EJBClientContext.getContextManager().setThreadDefault(ejbCtx);

            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
            InitialContext ctx = new InitialContext(props);
            String lookupName = "ejb:/" + MODULE_NAME + "/" + HeartbeatBean.class.getSimpleName() + "!" + Heartbeat.class.getName();
            Heartbeat bean = (Heartbeat) ctx.lookup(lookupName);

            // ====================================================================================
            // first call goes to connected node regardless of CustomClusterNodeSelector
            // ====================================================================================
            Result<Date> res = bean.pulse();
            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            // ====================================================================================
            // subsequent calls must be routed to the node selected by the CustomClusterNodeSelector
            // ====================================================================================
            callBeanOnNode(bean, NODE_4);
            callBeanOnNode(bean, NODE_2);
            callBeanOnNode(bean, NODE_4);
            callBeanOnNode(bean, NODE_4);
            callBeanOnNode(bean, NODE_1);
            callBeanOnNode(bean, NODE_3);

            ctx.close();
        } finally {
            if ((BKP != null)) {
                EJBClientContext.getContextManager().setThreadDefault(BKP);
            }
        }
    }

    /**
     * Calls the bean on the specified node and checks the response is coming from the specified node
     */
    private void callBeanOnNode(Heartbeat bean, String node) {
        CustomClusterNodeSelector.PICK_NODE = node;
        Result<Date> res = bean.pulse();
        Assert.assertEquals(
                String.format("%s not being used by the client to select cluster node! Request was routed to node %s instead of node %s! (check affinity value in logs)"
                        , CustomClusterNodeSelector.class.getName()
                        , res.getNode()
                        , CustomClusterNodeSelector.PICK_NODE)
                , CustomClusterNodeSelector.PICK_NODE, res.getNode());
    }

    /**
     * Test archive containing the EJB to call
     */
    private static Archive<?> createDeployment(String moduleName) {
        return ShrinkWrap.create(JavaArchive.class, moduleName + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Heartbeat.class, HeartbeatBean.class, RandomUtils.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }
}