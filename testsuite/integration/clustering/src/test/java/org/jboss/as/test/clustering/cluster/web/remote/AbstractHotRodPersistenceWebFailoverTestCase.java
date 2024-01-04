/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.transaction.TransactionMode;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.rules.TestRule;

/**
 * Variation of {@link AbstractWebFailoverTestCase} using invalidation cache with HotRod-based store implementation referencing
 * {@literal remote-cache-container} configuration. Test runs against running genuine Infinispan Server instance.
 *
 * @author Radoslav Husar
 */
public abstract class AbstractHotRodPersistenceWebFailoverTestCase extends AbstractWebFailoverTestCase {

    @ClassRule
    public static final TestRule INFINISPAN_SERVER_RULE = InfinispanServerUtil.infinispanServerTestRule();

    static Archive<?> getDeployment(String deploymentName, String deploymentDescriptor) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName);
        war.addClasses(SimpleServlet.class, Mutable.class);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        war.setWebXML(AbstractWebFailoverTestCase.class.getPackage(), "web.xml");
        war.addAsWebInfResource(AbstractHotRodPersistenceWebFailoverTestCase.class.getPackage(), deploymentDescriptor, "distributable-web.xml");
        return war;
    }

    public AbstractHotRodPersistenceWebFailoverTestCase(String deploymentName) {
        super(deploymentName, CacheMode.INVALIDATION_SYNC, TransactionMode.NON_TRANSACTIONAL);
    }

    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder.node(THREE_NODES)
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence:add")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence/store=hotrod:add(remote-cache-container=web, cache-configuration=default, shared=true)")
                    .teardown("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence:remove")
                    ;
        }
    }
}
