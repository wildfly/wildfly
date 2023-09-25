/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import static org.jboss.as.test.clustering.ClusterTestUtil.execute;

import java.net.URL;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.transaction.TransactionMode;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.rules.TestRule;

/**
 * Variation of {@link AbstractWebFailoverTestCase} using hotrod-based session manager.
 * Test runs against running genuine Infinispan Server instance.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public abstract class AbstractHotRodWebFailoverTestCase extends AbstractWebFailoverTestCase {

    @ClassRule
    public static final TestRule INFINISPAN_SERVER_RULE = InfinispanServerUtil.infinispanServerTestRule();

    @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1)
    private ManagementClient client1;
    @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2)
    private ManagementClient client2;
    @ArquillianResource @OperateOnDeployment(DEPLOYMENT_3)
    private ManagementClient client3;

    private final String deploymentName;

    public AbstractHotRodWebFailoverTestCase(String deploymentName) {
        super(deploymentName, CacheMode.LOCAL, TransactionMode.NON_TRANSACTIONAL);
        this.deploymentName = deploymentName;
    }

    @Override
    public void testGracefulSimpleFailover(URL baseURL1, URL baseURL2, URL baseURL3) throws Exception {
        super.testGracefulSimpleFailover(baseURL1, baseURL2, baseURL3);

        String readResource = String.format("/subsystem=infinispan/remote-cache-container=web/remote-cache=%s:read-resource(include-runtime=true, recursive=true)", this.deploymentName);
        String resetStatistics = String.format("/subsystem=infinispan/remote-cache-container=web/remote-cache=%s:reset-statistics", this.deploymentName);

        ModelNode result = execute(this.client1, readResource);
        Assert.assertNotEquals(0L, result.get("hits").asLong());
        Assert.assertNotEquals(0L, result.get("writes").asLong());

        result = execute(this.client2, readResource);
        Assert.assertEquals(0L, result.get("hits").asLong());
        Assert.assertEquals(0L, result.get("writes").asLong());

        result = execute(this.client3, readResource);
        Assert.assertNotEquals(0L, result.get("hits").asLong());
        Assert.assertNotEquals(0L, result.get("writes").asLong());

        execute(this.client1, resetStatistics);
        execute(this.client2, resetStatistics);
        execute(this.client3, resetStatistics);

        // These metrics should have reset
        result = execute(this.client1, readResource);
        Assert.assertEquals(0L, result.get("hits").asLong());
        Assert.assertEquals(0L, result.get("writes").asLong());

        result = execute(this.client2, readResource);
        Assert.assertEquals(0L, result.get("hits").asLong());
        Assert.assertEquals(0L, result.get("writes").asLong());

        result = execute(this.client3, readResource);
        Assert.assertEquals(0L, result.get("hits").asLong());
        Assert.assertEquals(0L, result.get("writes").asLong());
    }
}
