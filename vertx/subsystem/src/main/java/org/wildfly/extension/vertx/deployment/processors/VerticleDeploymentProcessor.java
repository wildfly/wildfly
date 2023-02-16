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

import static org.wildfly.extension.vertx.deployment.VerticleDeploymentsMetaData.DEPLOY_OPTIONS;
import static org.wildfly.extension.vertx.deployment.VerticleDeploymentsMetaData.VERTICLE_CLASS;

import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.vertx.VertxProxy;
import org.wildfly.extension.vertx.VertxResourceDefinition;
import org.wildfly.extension.vertx.deployment.VerticleDeploymentService;
import org.wildfly.extension.vertx.deployment.VerticleDeploymentsMetaData;
import org.wildfly.extension.vertx.deployment.VertxDeploymentAttachment;
import org.wildfly.extension.vertx.logging.VertxLogger;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

/**
 *
 * Processor used to deploy the verticle into specified Vertx instance.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VerticleDeploymentProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.INSTALL;
    public static final int PRIORITY = 0x4000;

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final VerticleDeploymentsMetaData verticleDeploymentsMetaData = VertxDeploymentAttachment.getVertxDeploymentsMeta(deploymentUnit);
        if (verticleDeploymentsMetaData == null) {
            return;
        }
        // if vertx-deployment.json exists in both sub deployment and parent deployment, it goes through all files
        // to deploy. It may be possible to take one precedence or merge, but it can be done in the future if necessary.
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
                final JsonObject deployOption = vertxDeployment.getJsonObject(DEPLOY_OPTIONS, new JsonObject());
                ServiceBuilder<?> sb = context.getServiceTarget().addService(ServiceName.of(deploymentUnit.getServiceName(), verticleClass));
                Supplier<VertxProxy> vertxProxySupplier = sb.requires(VertxResourceDefinition.VERTX_RUNTIME_CAPABILITY.getCapabilityServiceName());
                VerticleDeploymentService service = new VerticleDeploymentService(verticleClass, vertxProxySupplier,
                        new DeploymentOptions((deployOption)), moduleClassLoader);
                sb.setInstance(service)
                  .setInitialMode(ServiceController.Mode.ACTIVE)
                  .install();
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }
}
