/*
 *  Copyright (c) 2024 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.wildfly.extension.vertx.processors;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 *
 * VertxDeployment marker class to flag if the DeploymentUnit has vertx deployment configuration defined.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public final class VertxDeploymentAttachment {

    /**
     * Marker attachment key, this will be attached to the parent deployment unit if any.
     */
    private static final AttachmentKey<Boolean> MARKER_ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    private VertxDeploymentAttachment() {
        // ignore
    }

    /**
     * Mark the deployment as a Vertx deployment.
     *
     * @param deployment to be marked
     */
    public static void attachVertxDeployments(final DeploymentUnit deployment) {
        getRootDeploymentUnit(deployment).putAttachment(MARKER_ATTACHMENT_KEY, true);
    }

    /**
     * If the the parent deploymentUnit is marked as a vertx deployment, it is used by
     * {@link org.wildfly.extension.vertx.processors.VertxDependenciesProcessor} to add vertx module dependencies.
     *
     * @param deploymentUnit the deployment unit
     * @return true if it is marked as a vertx deployment, false otherwise.
     */
    public static boolean isVertxDeployment(final DeploymentUnit deploymentUnit) {
        Boolean marker = getRootDeploymentUnit(deploymentUnit).getAttachment(MARKER_ATTACHMENT_KEY);
        return marker != null && marker;
    }

    /**
     * Removes the attachment from the deployment unit
     *
     * @param deploymentUnit the deployment unit
     */
    public static void removeVertxDeploymentsMeta(final DeploymentUnit deploymentUnit) {
        getRootDeploymentUnit(deploymentUnit).removeAttachment(MARKER_ATTACHMENT_KEY);
    }

    /**
     * Returns the parent of the given deployment unit if such a parent exists. If the given deployment unit is the parent
     * deployment unit, it is returned.
     */
    public static DeploymentUnit getRootDeploymentUnit(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return deploymentUnit;
        }
        return deploymentUnit.getParent();
    }

}
