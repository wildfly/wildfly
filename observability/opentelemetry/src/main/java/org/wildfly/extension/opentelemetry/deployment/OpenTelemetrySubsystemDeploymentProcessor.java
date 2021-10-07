/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.opentelemetry.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SERVICE_NAME;
import static org.wildfly.extension.opentelemetry.deployment.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import io.opentelemetry.api.OpenTelemetry;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.weld.WeldCapability;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.wildfly.extension.opentelemetry.OpenTelemetryHolder;
import org.wildfly.extension.opentelemetry.api.OpenTelemetryCdiExtension;
import org.wildfly.security.manager.WildFlySecurityManager;

public class OpenTelemetrySubsystemDeploymentProcessor implements DeploymentUnitProcessor {
    private final OpenTelemetryHolder holder;

    public OpenTelemetrySubsystemDeploymentProcessor(OpenTelemetryHolder holder) {
        this.holder = holder;
    }

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        OTEL_LOGGER.processingDeployment();
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        try {
            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                // Jakarta RESTful Web Services require Jakarta Contexts and Dependency Injection. Without Jakarta
                // Contexts and Dependency Injection, there's no integration needed
                OTEL_LOGGER.noCdiDeployment();
                return;
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            // We should not be here since the subsystem depends on weld capability. Just in case ...
            throw OTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        }
        setupOtelCdiBeans(deploymentPhaseContext, support);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void setupOtelCdiBeans(DeploymentPhaseContext deploymentPhaseContext, CapabilityServiceSupport support) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        final ClassLoader initialCl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ModuleClassLoader moduleCL = module.getClassLoader();

        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(moduleCL);
            String serviceName = holder.config.serviceName != null ? holder.config.serviceName : getServiceName(deploymentUnit);

            final OpenTelemetry openTelemetry =
                    OpenTelemetryCdiExtension.registerApplicationOpenTelemetryBean(moduleCL, holder.getOpenTelemetry());
            OpenTelemetryCdiExtension.registerApplicationTracer(moduleCL, openTelemetry.getTracer(serviceName));
            OTEL_LOGGER.registeringTracer(serviceName);
        } catch (SecurityException | IllegalArgumentException ex) {
            OTEL_LOGGER.errorResolvingTracer(ex);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(initialCl);
        }
    }

    private String getServiceName(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        String serviceName = null;

        if (null == jbossWebMetaData) {
            // nothing to do here
            serviceName = "";
        } else {
            if (jbossWebMetaData.getContextParams() != null) {
                for (ParamValueMetaData param : jbossWebMetaData.getContextParams()) {
                    if (SERVICE_NAME.equals(param.getParamName())) {
                        serviceName = param.getParamValue();
                    }
                }
            }

            if (null == serviceName || serviceName.isEmpty()) {
                if (null != deploymentUnit.getParent()) {
                    // application.ear!module.war
                    serviceName = deploymentUnit.getParent().getServiceName().getSimpleName()
                            + "!"
                            + deploymentUnit.getServiceName().getSimpleName();
                } else {
                    serviceName = deploymentUnit.getServiceName().getSimpleName();
                }

                OTEL_LOGGER.serviceNameDerivedFromDeploymentUnit(serviceName);
            }
        }

        return serviceName;
    }

    private JBossWebMetaData getJBossWebMetaData(DeploymentUnit deploymentUnit) {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (null == warMetaData) {
            // not a web deployment, nothing to do here...
            return null;
        }
        return warMetaData.getMergedJBossWebMetaData();
    }
}
