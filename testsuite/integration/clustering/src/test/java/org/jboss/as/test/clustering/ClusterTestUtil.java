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

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.server.security.ServerPermission;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;

/**
 * Utility class for clustering tests.
 *
 * @author Radoslav Husar
 */
public class ClusterTestUtil {

    /**
     * <em>Note that should you need to manually add an extra set of permissions, the following permission is required for this utility to work within
     * security manager:</em>
     *
     * <pre>{@code
     * war.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new ServerPermission("getCurrentServiceContainer")), "permissions.xml");
     * }</pre>
     */
    public static <A extends Archive<A> & ClassContainer<A> & ManifestContainer<A>> A addTopologyListenerDependencies(A archive) {
        archive.addClasses(TopologyChangeListener.class, TopologyChangeListenerBean.class, TopologyChangeListenerServlet.class);
        archive.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.as.clustering.common, org.jboss.as.controller, org.jboss.as.server, org.infinispan, org.wildfly.clustering.infinispan.spi\n"));
        archive.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new ServerPermission("getCurrentServiceContainer")), "permissions.xml");
        return archive;
    }

    public static void establishTopology(EJBDirectory directory, String container, String cache, String... nodes) throws Exception {
        TopologyChangeListener listener = directory.lookupStateless(TopologyChangeListenerBean.class, TopologyChangeListener.class);
        listener.establishTopology(container, cache, TopologyChangeListener.DEFAULT_TIMEOUT, nodes);
    }

    // Model management convenience methods

    public static void executeOnNodesAndReload(String cli, ManagementClient... clients) throws Exception {
        ModelNode request = CLITestUtil.getCommandContext().buildRequest(cli);

        for (ManagementClient client : clients) {
            client.getControllerClient().execute(request);
            ServerReload.executeReloadAndWaitForCompletion(client.getControllerClient(), ServerReload.TIMEOUT, false, client.getMgmtAddress(), client.getMgmtPort());
        }
    }

    private ClusterTestUtil() {
    }
}
