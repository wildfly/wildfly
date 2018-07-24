/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.opentracing;

import io.smallrye.opentracing.SmallRyeTracingDynamicFeature;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.wildfly.microprofile.opentracing.smallrye.TracerInitializer;
import org.wildfly.microprofile.opentracing.smallrye.TracingCDIExtension;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.util.ArrayList;
import java.util.List;

public class TracingDeploymentProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) {
        TracingExtensionLogger.ROOT_LOGGER.processingDeployment();
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            // SmallRye JAX-RS requires CDI. Without CDI, there's no integration needed
            TracingExtensionLogger.ROOT_LOGGER.noCdiDeployment();
            return;
        }

        addListeners(deploymentUnit);
        addJaxRsIntegration(deploymentUnit);
        addCDIExtension(deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
    }

    private void addListeners(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if (null == jbossWebMetaData) {
            // nothing to do here
            return;
        }

        TracingExtensionLogger.ROOT_LOGGER.registeringTracerInitializer();

        String serviceName = getServiceName(deploymentUnit);
        ParamValueMetaData serviceNameContextParameter = new ParamValueMetaData();
        serviceNameContextParameter.setParamName(TracerInitializer.SMALLRYE_OPENTRACING_SERVICE_NAME);
        serviceNameContextParameter.setParamValue(serviceName);
        addContextParameter(jbossWebMetaData, serviceNameContextParameter);

        ListenerMetaData listenerMetaData = new ListenerMetaData();
        listenerMetaData.setListenerClass(TracerInitializer.class.getName());

        List<ListenerMetaData> listeners = jbossWebMetaData.getListeners();
        if (null == listeners) {
            listeners = new ArrayList<>();
        }
        listeners.add(listenerMetaData);
        jbossWebMetaData.setListeners(listeners);
    }

    private void addJaxRsIntegration(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if (null == jbossWebMetaData) {
            // nothing to do here
            return;
        }

        TracingExtensionLogger.ROOT_LOGGER.registeringJaxRs();

        ParamValueMetaData restEasyProvider = new ParamValueMetaData();
        restEasyProvider.setParamName("resteasy.providers");
        restEasyProvider.setParamValue(SmallRyeTracingDynamicFeature.class.getName());
        addContextParameter(jbossWebMetaData, restEasyProvider);
    }

    private void addCDIExtension(DeploymentUnit deploymentUnit) {
        TracingExtensionLogger.ROOT_LOGGER.registeringCDIExtension();

        WeldPortableExtensions extensions = WeldPortableExtensions.getPortableExtensions(deploymentUnit);
        extensions.registerExtensionInstance(new TracingCDIExtension(), deploymentUnit);
    }

    private JBossWebMetaData getJBossWebMetaData(DeploymentUnit deploymentUnit) {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (null == warMetaData) {
            // not a web deployment, nothing to do here...
            return null;
        }

        return warMetaData.getMergedJBossWebMetaData();
    }

    private void addContextParameter(JBossWebMetaData jbossWebMetaData, ParamValueMetaData restEasyProvider) {
        List<ParamValueMetaData> contextParams = jbossWebMetaData.getContextParams();
        if (null == contextParams) {
            contextParams = new ArrayList<>();
        }
        contextParams.add(restEasyProvider);
        jbossWebMetaData.setContextParams(contextParams);
    }

    private String getServiceName(DeploymentUnit deploymentUnit) {
        String serviceName = WildFlySecurityManager.getPropertyPrivileged("JAEGER_SERVICE_NAME", "");
        if (null == serviceName || serviceName.isEmpty()) {
            serviceName = WildFlySecurityManager.getEnvPropertyPrivileged("JAEGER_SERVICE_NAME", "");
        }

        if (null == serviceName || serviceName.isEmpty()) {
            TracingExtensionLogger.ROOT_LOGGER.serviceNameDerivedFromDeploymentUnit(serviceName);
            serviceName = deploymentUnit.getServiceName().getSimpleName();
        }

        return serviceName;
    }
}
