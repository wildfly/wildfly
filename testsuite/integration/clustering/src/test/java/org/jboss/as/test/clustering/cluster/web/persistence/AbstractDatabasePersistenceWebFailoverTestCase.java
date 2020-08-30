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

package org.jboss.as.test.clustering.cluster.web.persistence;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.transaction.TransactionMode;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.ClusterDatabaseTestUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.shared.CLIServerSetupTask;
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
        super(deploymentName, CacheMode.INVALIDATION_SYNC, TransactionMode.TRANSACTIONAL);
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

    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder.node(THREE_NODES)
                    .setup("/subsystem=datasources/data-source=web-sessions-ds:add(jndi-name=\"java:jboss/datasources/web-sessions-ds\", enabled=true, use-java-context=true, connection-url=\"jdbc:h2:tcp://localhost:%s/./web-sessions\", driver-name=h2", DB_PORT)
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence:add")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/store=jdbc:add(data-source=web-sessions-ds, fetch-state=false, purge=false, passivation=false, shared=true)")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/component=transaction:add(mode=BATCH)")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/component=locking:add(isolation=REPEATABLE_READ)")
                    .teardown("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence:remove")
                    .teardown("/subsystem=datasources/data-source=web-sessions-ds:remove");
        }
    }
}
