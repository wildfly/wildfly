/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;
import java.util.Set;

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
        archive.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan.core\n"));
        archive.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("getClassLoader"), new PropertyPermission("jboss.node.name", "read")), "permissions.xml");
        return archive;
    }

    public static void establishTopology(EJBDirectory directory, String container, String cache, Set<String> topology) throws Exception {
        TopologyChangeListener listener = directory.lookupStateless(TopologyChangeListenerBean.class, TopologyChangeListener.class);
        listener.establishTopology(container, cache, topology, TopologyChangeListener.DEFAULT_TIMEOUT);
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
