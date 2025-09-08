/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.persistence;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.transaction.TransactionMode;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.ClusterDatabaseTestUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * Tests session failover where sessions are stored in a shared H2 database using invalidation cache mode.
 *
 * @author Tomas Remes
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
public abstract class AbstractDatabasePersistenceWebFailoverTestCase extends AbstractWebFailoverTestCase {

    public AbstractDatabasePersistenceWebFailoverTestCase(String deploymentName) {
        super(deploymentName, CacheMode.INVALIDATION_SYNC, TransactionMode.NON_TRANSACTIONAL);
    }

    static Archive<?> getDeployment(String deploymentName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName);
        war.addClasses(SimpleServlet.class, Mutable.class);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        war.setWebXML(AbstractWebFailoverTestCase.class.getPackage(), "web.xml");
        war.addAsWebInfResource(AbstractDatabasePersistenceWebFailoverTestCase.class.getPackage(), "distributable-web.xml", "distributable-web.xml");
        return war;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusterDatabaseTestUtil.startH2();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusterDatabaseTestUtil.stopH2();
    }

    public static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(NODE_1_2_3, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                                .add("/subsystem=datasources/data-source=web-sessions:add(jndi-name=\"java:jboss/datasources/web-sessions-ds\", enabled=true, use-java-context=true, connection-url=\"jdbc:h2:tcp://localhost:%s/./web-sessions;VARIABLE_BINARY=TRUE\", driver-name=h2", DB_PORT)
                                .add("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence:add")
                                .add("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/component=expiration:add(interval=0)")
                                .add("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/component=locking:add(isolation=REPEATABLE_READ)")
                                .add("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/component=transaction:add(mode=BATCH)")
                                .add("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/store=jdbc:add(data-source=web-sessions, shared=true)")
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                                .add("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence:remove")
                                .add("/subsystem=datasources/data-source=web-sessions:remove")
                            .endBatch()
                            .build())
                    .build());
        }
    }
}
