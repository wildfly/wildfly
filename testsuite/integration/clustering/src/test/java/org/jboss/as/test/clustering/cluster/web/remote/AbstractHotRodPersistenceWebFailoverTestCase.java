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

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.transaction.TransactionMode;
import org.jboss.as.test.clustering.ClusterTestUtil;
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
    public static final TestRule INFINISPAN_SERVER_RULE = infinispanServerTestRule();

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
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence/store=hotrod:add(remote-cache-container=web, cache-configuration=default, fetch-state=false, purge=false, passivation=false, shared=true)")
                    .teardown("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence:remove")
                    ;
        }
    }
}
