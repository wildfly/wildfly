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

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.wildfly.microprofile.opentracing.smallrye.TracerInitializer;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.util.ArrayList;
import java.util.List;

public class TracingDeploymentProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) {
        TracingExtensionLogger.ROOT_LOGGER.processingDeployment();
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            // SmallRye JAX-RS requires CDI. Without CDI, there's no integration needed
            TracingExtensionLogger.ROOT_LOGGER.noCdiDeployment();
            return;
        }

        setServiceName(deploymentUnit);
        addListeners(deploymentUnit);
    }

    private void addListeners(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if (null == jbossWebMetaData) {
            // nothing to do here
            return;
        }

        TracingExtensionLogger.ROOT_LOGGER.registeringTracerInitializer();

        ListenerMetaData listenerMetaData = new ListenerMetaData();
        listenerMetaData.setListenerClass(TracerInitializer.class.getName());

        List<ListenerMetaData> listeners = jbossWebMetaData.getListeners();
        if (null == listeners) {
            listeners = new ArrayList<>();
        }
        listeners.add(listenerMetaData);
        jbossWebMetaData.setListeners(listeners);
    }

    private void setServiceName(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if (null == jbossWebMetaData) {
            // nothing to do here
            return;
        }

        String serviceName = getServiceName(deploymentUnit);
        ParamValueMetaData serviceNameContextParameter = new ParamValueMetaData();
        serviceNameContextParameter.setParamName(TracerInitializer.SMALLRYE_OPENTRACING_SERVICE_NAME);
        serviceNameContextParameter.setParamValue(serviceName);
        addContextParameter(jbossWebMetaData, serviceNameContextParameter);
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
            if (null != deploymentUnit.getParent()) {
                // application.ear!module.war
                serviceName = deploymentUnit.getParent().getServiceName().getSimpleName()
                        + "!"
                        + deploymentUnit.getServiceName().getSimpleName();
            } else {
                serviceName = deploymentUnit.getServiceName().getSimpleName();
            }

            TracingExtensionLogger.ROOT_LOGGER.serviceNameDerivedFromDeploymentUnit(serviceName);
        }

        return serviceName;
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
    }
}
