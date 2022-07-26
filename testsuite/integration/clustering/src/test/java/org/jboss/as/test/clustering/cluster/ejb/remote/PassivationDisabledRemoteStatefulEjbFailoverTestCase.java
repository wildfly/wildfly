package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.PropertyPermission;
import jakarta.ejb.NoSuchEJBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.PassivationDisabledStatefulIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Test the following properties of passivation-disabled SFSB when deployed in a cluster:
 * - stickiness of passivation-disabled SFSB to the node its is created on, as well as
 * - verify that it does not fail over to another node in the cluster when the node it is created on goes down
 * .
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class PassivationDisabledRemoteStatefulEjbFailoverTestCase extends AbstractClusteringTestCase {
    private static final int COUNT = 20;
    private static final long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);
    private static final String MODULE_NAME = PassivationDisabledRemoteStatefulEjbFailoverTestCase.class.getSimpleName();

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

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, PassivationDisabledStatefulIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    private final ExceptionSupplier<EJBDirectory, Exception> directoryProvider;

    public PassivationDisabledRemoteStatefulEjbFailoverTestCase() {
        this.directoryProvider = () -> new RemoteEJBDirectory(MODULE_NAME);
    }

    @Test
    public void test() throws Exception {
        try (EJBDirectory directory = this.directoryProvider.get()) {
            Incrementor bean = directory.lookupStateful(PassivationDisabledStatefulIncrementorBean.class, Incrementor.class);

            Result<Integer> result = bean.increment();
            String target = result.getNode();
            int count = 1;

            Assert.assertEquals(count++, result.getValue().intValue());

            // Bean should retain strong affinity for this node
            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals(count++, result.getValue().intValue());
                Assert.assertEquals(String.valueOf(i), target, result.getNode());
            }

            undeploy(this.findDeployment(target));

            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            try {
                result = bean.increment();

                // Bean should fail to failover to other node
                Assert.fail(result.getNode());
            } catch (NoSuchEJBException e) {
                // Failover should fail
            }
        }
    }
}
