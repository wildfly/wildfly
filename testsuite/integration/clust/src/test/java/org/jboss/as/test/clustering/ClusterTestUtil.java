/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering;

import javax.naming.NamingException;

import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;

/**
 * Utility class for cluster test.
 *
 * @author Radoslav Husar
 * @version September 2012
 */
public class ClusterTestUtil {

    public static void waitForReplication(int millis) {
        if (ClusteringTestConstants.TEST_CACHE_MODE.equalsIgnoreCase("SYNC")) {
            // In case the replication is sync, we do not need to wait for the replication to happen.
            return;
        }
        // TODO: Instead of dummy waiting, we could attach a listener and notify the test framework the replication has happened. millis value can be used as timeout in that case.
        try {
            Thread.sleep(millis);
        } catch (InterruptedException iex) {
        }
    }

    public static <A extends Archive<A> & ClassContainer<A> & ManifestContainer<A>> A addTopologyListenerDependencies(A archive) {
        archive.addClasses(TopologyChangeListener.class, TopologyChangeListenerBean.class, TopologyChangeListenerServlet.class);
        archive.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.msc, org.jboss.as.clustering.common, org.jboss.as.server, org.infinispan\n"));
        return archive;
    }

    public static void establishTopology(EJBDirectory directory, String container, String cache, String... nodes) throws NamingException, InterruptedException {
        TopologyChangeListener listener = directory.lookupStateless(TopologyChangeListenerBean.class, TopologyChangeListener.class);
        listener.establishTopology(container, cache, nodes);
    }

    private ClusterTestUtil() {
    }
}
