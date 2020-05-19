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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.junit.Assert;

/**
 * Utility class for clustering tests.
 *
 * @author Radoslav Husar
 */
public class ClusterTestUtil {

    public static <A extends Archive<A> & ClassContainer<A> & ManifestContainer<A>> A addTopologyListenerDependencies(A archive) {
        archive.addClasses(TopologyChangeListener.class, TopologyChangeListenerBean.class, TopologyChangeListenerServlet.class);
        archive.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan\n"));
        return archive;
    }

    public static void establishTopology(EJBDirectory directory, String container, String cache, String... nodes) throws Exception {
        TopologyChangeListener listener = directory.lookupStateless(TopologyChangeListenerBean.class, TopologyChangeListener.class);
        listener.establishTopology(container, cache, TopologyChangeListener.DEFAULT_TIMEOUT, nodes);
    }

    // Model management convenience methods

    public static ModelNode execute(ManagementClient client, String request) throws Exception {
        ModelNode operation = CLITestUtil.getCommandContext().buildRequest(request);
        ModelNode result = client.getControllerClient().execute(operation);
        Assert.assertEquals(result.toString(), ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asStringOrNull());
        return result.get(ModelDescriptionConstants.RESULT);
    }

    private ClusterTestUtil() {
    }
}
