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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.CLIServerSetupTask;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Variation of {@link AbstractWebFailoverTestCase} using invalidation cache with HotRod-based store implementation referencing
 * {@literal remote-cache-container} configuration. Test runs against running genuine Infinispan Server instance.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup(HotRodPersistenceWebFailoverTestCase.ServerSetupTask.class)
public class HotRodPersistenceWebFailoverTestCase extends AbstractWebFailoverTestCase {

    private static final String DEPLOYMENT_NAME = HotRodPersistenceWebFailoverTestCase.class.getSimpleName() + ".war";

    public HotRodPersistenceWebFailoverTestCase() {
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
        war.addAsWebInfResource(HotRodPersistenceWebFailoverTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        return war;
    }

    @Override
    public void beforeTestMethod() {
        // Also start the Infinispan Server instance
        NodeUtil.start(controller, INFINISPAN_SERVER_1);

        NodeUtil.start(this.controller, this.nodes);
        NodeUtil.deploy(this.deployer, this.deployments);
    }

    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            builder.node(THREE_NODES)
                    // remote-cache-container=web-sessions
                    .setup("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server-1:add(port=11622,host=%s)", TESTSUITE_NODE0)
                    .setup("/subsystem=infinispan/remote-cache-container=web-sessions:add(default-remote-cluster=infinispan-server-cluster)")
                    .setup("/subsystem=infinispan/remote-cache-container=web-sessions/remote-cluster=infinispan-server-cluster:add(socket-bindings=[infinispan-server-1])")
                    // store=hotrod
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence:add")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence/store=hotrod:add(remote-cache-container=web-sessions,cache-configuration=default,fetch-state=false,purge=false,passivation=false,shared=true")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence/component=transaction:add(mode=BATCH)")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence/component=locking:add(isolation=REPEATABLE_READ)")
                    .teardown("/subsystem=infinispan/cache-container=web/invalidation-cache=hotrod-persistence:remove")
                    .teardown("/subsystem=infinispan/remote-cache-container=web-sessions:remove")
                    .teardown("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server-1:remove")
            ;
        }
    }
}
