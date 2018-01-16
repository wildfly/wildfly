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

import java.net.URL;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.transaction.TransactionMode;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.CLIServerSetupTask;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Tests session failover where sessions are stored in a shared database (H2 database in AUTO_SERVER mode) using invalidation cache mode.
 *
 * @author Tomas Remes
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup(DatabasePersistenceWebFailoverTestCase.ServerSetupTask.class)
public class DatabasePersistenceWebFailoverTestCase extends AbstractWebFailoverTestCase {
    private static final String DEPLOYMENT_NAME = DatabasePersistenceWebFailoverTestCase.class.getSimpleName() + ".war";

    public DatabasePersistenceWebFailoverTestCase() {
        super(DEPLOYMENT_NAME, CacheMode.INVALIDATION_SYNC, TransactionMode.NON_TRANSACTIONAL);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_3)
    public static Archive<?> deployment3() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addClasses(SimpleServlet.class, Mutable.class);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        war.setWebXML(AbstractWebFailoverTestCase.class.getPackage(), "web.xml");
        war.addAsWebInfResource(DatabasePersistenceWebFailoverTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        return war;
    }

    @Override
    public void testGracefulSimpleFailover(URL baseURL1, URL baseURL2, URL baseURL3) {
        // TODO rework to use external database process since H2's AUTO_SERVER doesn't handle server restarts reliably
    }

    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            builder.node(THREE_NODES)
                    .setup("/subsystem=datasources/data-source=web-sessions-ds:add(jndi-name=\"java:jboss/datasources/web-sessions-ds\",enabled=true,use-java-context=true,connection-url=\"jdbc:h2:file:./target/h2/web-sessions;AUTO_SERVER=true;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;\",driver-name=h2")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence:add")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/store=jdbc:add(data-source=web-sessions-ds,fetch-state=false,purge=false,passivation=false,shared=true")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/component=transaction:add(mode=BATCH)")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/component=locking:add(isolation=REPEATABLE_READ)")
                    .teardown("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence:remove")
                    .teardown("/subsystem=datasources/data-source=web-sessions-ds:remove");
        }
    }
}
