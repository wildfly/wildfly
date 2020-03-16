/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.web.remote;

import static org.jboss.as.test.clustering.ClusterTestUtil.execute;

import java.net.URL;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.transaction.TransactionMode;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * Variation of {@link AbstractWebFailoverTestCase} using hotrod-based session manager.
 * Test runs against running genuine Infinispan Server instance.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public abstract class AbstractHotRodWebFailoverTestCase extends AbstractWebFailoverTestCase {

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
    public void beforeTestMethod() {
        // Also start the Infinispan Server instance
        NodeUtil.start(this.controller, INFINISPAN_SERVER_1);

        NodeUtil.start(this.controller, this.nodes);
        NodeUtil.deploy(this.deployer, this.deployments);
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
