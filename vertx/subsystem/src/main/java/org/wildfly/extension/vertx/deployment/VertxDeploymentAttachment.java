/*
 * Copyright (C) 2022 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.vertx.deployment;

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
     * VertxDeploymentsMeta attachment key.
     */
    private static final AttachmentKey<VerticleDeploymentsMetaData> ATTACHMENT_KEY = AttachmentKey.create(VerticleDeploymentsMetaData.class);

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
    public static void attachVertxDeployments(final DeploymentUnit deployment, final VerticleDeploymentsMetaData vertxDeployments) {
        deployment.putAttachment(ATTACHMENT_KEY, vertxDeployments);
        getRootDeploymentUnit(deployment).putAttachment(MARKER_ATTACHMENT_KEY, true);
    }

    /**
     * If the the parent deploymentUnit is marked as a vertx deployment, it is used by
     * {@link org.wildfly.extension.vertx.deployment.processors.VertxDependenciesProcessor} to add vertx module dependencies.
     *
     * @param deploymentUnit the deployment unit
     * @return true if it is marked as a vertx deployment, false otherwise.
     */
    public static boolean isVertxDeployment(final DeploymentUnit deploymentUnit) {
        Boolean marker = getRootDeploymentUnit(deploymentUnit).getAttachment(MARKER_ATTACHMENT_KEY);
        return marker != null && marker;
    }

    /**
     * Gets the vertx deployment meta from the deployment unit.
     *
     * @param deploymentUnit the deployment unit
     * @return the vertx deployment meta or null
     */
    public static VerticleDeploymentsMetaData getVertxDeploymentsMeta(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getAttachment(ATTACHMENT_KEY);
    }

    /**
     * Removes the attachment from the deployment unit
     *
     * @param deploymentUnit the deployment unit
     */
    public static void removeVertxDeploymentsMeta(final DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(ATTACHMENT_KEY);
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
