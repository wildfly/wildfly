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

import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * @author Paul Ferraro
 */
@ServerSetup(CoarseDatabasePersistenceWebFailoverTestCase.ServerSetupTask.class)
@org.junit.Ignore("Fails intermittently due to ISPN-10029")
public class CoarseDatabasePersistenceWebFailoverTestCase extends AbstractDatabasePersistenceWebFailoverTestCase {

    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder.node(THREE_NODES)
                    .setup("/subsystem=datasources/data-source=web-sessions-ds:add(jndi-name=\"java:jboss/datasources/web-sessions-ds\",enabled=true,use-java-context=true,connection-url=\"jdbc:h2:file:./target/h2/web-sessions;AUTO_SERVER=true;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;\",driver-name=h2")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence:add")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/store=jdbc:add(data-source=web-sessions-ds,fetch-state=false,purge=false,passivation=false,shared=true")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/component=transaction:add(mode=BATCH)")
                    .setup("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence/component=locking:add(isolation=REPEATABLE_READ)")
                    .setup("/subsystem=distributable-web/infinispan-session-management=database:add(cache-container=web, cache=database-persistence, granularity=SESSION)")
                    .setup("/subsystem=distributable-web/infinispan-session-management=database/affinity=local:add()")
                    .teardown("/subsystem=distributable-web/infinispan-session-management=database:remove")
                    .teardown("/subsystem=infinispan/cache-container=web/invalidation-cache=database-persistence:remove")
                    .teardown("/subsystem=datasources/data-source=web-sessions-ds:remove");
        }
    }
}
