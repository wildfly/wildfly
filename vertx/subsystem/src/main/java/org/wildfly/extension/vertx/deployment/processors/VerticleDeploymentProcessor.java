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
package org.wildfly.extension.vertx.deployment.processors;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.vertx.VertxConstants;
import org.wildfly.extension.vertx.deployment.VerticleDeploymentService;
import org.wildfly.extension.vertx.deployment.VertxDeploymentAttachment;
import org.wildfly.extension.vertx.deployment.VerticleDeploymentsMetaData;
import org.wildfly.extension.vertx.logging.VertxLogger;

import java.util.List;

import static org.wildfly.extension.vertx.deployment.VerticleDeploymentsMetaData.DEFAULT_VERTX_NAME;
import static org.wildfly.extension.vertx.deployment.VerticleDeploymentsMetaData.DEPLOY_OPTIONS;
import static org.wildfly.extension.vertx.deployment.VerticleDeploymentsMetaData.VERTICLE_CLASS;
import static org.wildfly.extension.vertx.deployment.VerticleDeploymentsMetaData.VERTX_JNDI_NAME;
import static org.wildfly.extension.vertx.deployment.VerticleDeploymentsMetaData.VERTX_NAME;

/**
 *
 * Processor used to deploy the verticle into specified Vertx instance.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VerticleDeploymentProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.INSTALL;
    public static final int PRIORITY = 0x4000;

    private static final ServiceName serviceNameBase = ServiceName.of(ServiceName.JBOSS, "deployment");

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final VerticleDeploymentsMetaData verticleDeploymentsMetaData = VertxDeploymentAttachment.getVertxDeploymentsMeta(deploymentUnit);
        if (verticleDeploymentsMetaData == null) {
            return;
        }
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ClassLoader moduleClassLoader = module.getClassLoader();
        List<JsonObject> vertxDeployments = verticleDeploymentsMetaData.getVerticleDeployments();
        if (vertxDeployments.size() > 0) {
            final String deploymentName = deploymentUnit.getName();
            // gets the vertx from initial context, then deploy the verticles.
            for (JsonObject vertxDeployment: vertxDeployments) {
                final String verticleClass = vertxDeployment.getString(VERTICLE_CLASS);
                if (verticleClass == null || verticleClass.length() == 0) {
                    throw VertxLogger.VERTX_LOGGER.noVerticleClassDefined(deploymentName);
                }
                final String vertxName = vertxDeployment.getString(VERTX_NAME, DEFAULT_VERTX_NAME);
                String jndiName = vertxDeployment.getString(VERTX_JNDI_NAME);
                if (jndiName == null || jndiName.length() == 0) {
                    jndiName = VertxConstants.DEFAULT_JNDI_PREFIX + vertxName;
                }

                final JsonObject deployOption = vertxDeployment.getJsonObject(DEPLOY_OPTIONS, new JsonObject());
                VerticleDeploymentService service = new VerticleDeploymentService(verticleClass, jndiName,
                        new DeploymentOptions((deployOption)), moduleClassLoader);
                context.getServiceTarget().addService(ServiceName.of(serviceNameBase, deploymentName, verticleClass))
                        .setInstance(service)
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }
}
