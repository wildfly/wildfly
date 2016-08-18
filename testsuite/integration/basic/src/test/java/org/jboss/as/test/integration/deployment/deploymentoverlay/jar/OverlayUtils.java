/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.deployment.deploymentoverlay.jar;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;

/**
 * @author baranowb
 * @author lgao
 */
public final class OverlayUtils {

    public static void setupOverlay(final ManagementClient managementClient, final String deployment, final String overlayName, final Map<String, String> overlay) throws Exception {

        // create overlay
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, overlayName);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        for (Map.Entry<String, String> overlayItem : overlay.entrySet()) {
            // add content
            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(new ModelNode());
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UPLOAD_DEPLOYMENT_BYTES);
            op.get(ModelDescriptionConstants.BYTES).set(overlayItem.getValue().getBytes());
            ModelNode result = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            // link content to specific file
            op = new ModelNode();
            ModelNode addr = new ModelNode();
            addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, overlayName);
            addr.add(ModelDescriptionConstants.CONTENT, overlayItem.getKey());
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.HASH).set(result);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        }

        // add link
        op = new ModelNode();
        ModelNode addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, overlayName);
        addr.add(ModelDescriptionConstants.DEPLOYMENT, deployment);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT, deployment);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REDEPLOY);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }

    public static void setupOverlay(final ManagementClient managementClient, final String deployment, final String overlayName, final String overlayPath, final String overlayedContent) throws Exception {
        setupOverlay(managementClient, deployment, overlayName, Collections.singletonMap(overlayPath, overlayedContent));
    }

    public static void removeOverlay(final ManagementClient managementClient, final String deployment, final String overlayName, final Set<String> overlayPaths) throws Exception {
        for (String overlayPath : overlayPaths) {
            removeContentItem(managementClient, overlayName, overlayPath);
        }
        removeDeploymentItem(managementClient, overlayName, deployment);

        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, overlayName);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }

    public static void removeOverlay(final ManagementClient managementClient, final String deployment, final String overlayName, final String overlayPath) throws Exception {
        removeOverlay(managementClient, deployment, overlayName, Collections.singleton(overlayPath));
    }

    protected static void removeContentItem(final ManagementClient managementClient, final String w, final String a) throws IOException, MgmtOperationException {
        final ModelNode op;
        final ModelNode addr;
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, w);
        addr.add(ModelDescriptionConstants.CONTENT, a);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }


    protected static void removeDeploymentItem(final ManagementClient managementClient, final String w, final String a) throws IOException, MgmtOperationException {
        final ModelNode op;
        final ModelNode addr;
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, w);
        addr.add(ModelDescriptionConstants.DEPLOYMENT, a);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }

}
