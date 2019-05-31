/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;

/**
 * @author Paul Ferraro
 */
@ServerSetup({ AbstractDatabasePersistenceWebFailoverTestCase.ServerSetupTask.class, CoarseDatabasePersistenceWebFailoverTestCase.ServerSetupTask.class })
public class CoarseDatabasePersistenceWebFailoverTestCase extends AbstractDatabasePersistenceWebFailoverTestCase {

    private static final String DEPLOYMENT_NAME = CoarseDatabasePersistenceWebFailoverTestCase.class.getSimpleName() + ".war";

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
        return getDeployment(DEPLOYMENT_NAME);
    }

    public CoarseDatabasePersistenceWebFailoverTestCase() {
        super(DEPLOYMENT_NAME);
    }

    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder.node(THREE_NODES)
                    .setup("/subsystem=distributable-web/infinispan-session-management=database:add(cache-container=web, cache=database-persistence, granularity=SESSION)")
                    .setup("/subsystem=distributable-web/infinispan-session-management=database/affinity=local:add()")
                    .teardown("/subsystem=distributable-web/infinispan-session-management=database:remove")
                    ;
        }
    }
}
