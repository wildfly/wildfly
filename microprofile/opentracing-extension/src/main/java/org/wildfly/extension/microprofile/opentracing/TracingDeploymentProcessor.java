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

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.DEFAULT_TRACER_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.TRACER_CAPABILITY;
import static org.wildfly.extension.microprofile.opentracing.TracingExtensionLogger.ROOT_LOGGER;
import static org.wildfly.microprofile.opentracing.smallrye.TracerConfigurationConstants.SMALLRYE_OPENTRACING_SERVICE_NAME;
import static org.wildfly.microprofile.opentracing.smallrye.TracerConfigurationConstants.SMALLRYE_OPENTRACING_TRACER;
import static org.wildfly.microprofile.opentracing.smallrye.TracerConfigurationConstants.SMALLRYE_OPENTRACING_TRACER_CONFIGURATION;
import static org.wildfly.microprofile.opentracing.smallrye.TracerConfigurationConstants.SMALLRYE_OPENTRACING_TRACER_MANAGED;
import static org.wildfly.microprofile.opentracing.smallrye.TracerConfigurationConstants.TRACER_CONFIGURATION;
import static org.wildfly.microprofile.opentracing.smallrye.TracerConfigurationConstants.TRACER_CONFIGURATION_NAME;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
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
import org.wildfly.security.manager.WildFlySecurityManager;

import java.util.ArrayList;
import java.util.List;
import org.jboss.msc.service.ServiceName;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Module;
import org.wildfly.microprofile.opentracing.smallrye.TracingCDIExtension;
import org.wildfly.microprofile.opentracing.smallrye.TracingLogger;
import org.wildfly.microprofile.opentracing.smallrye.WildFlyTracerFactory;

public class TracingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final AttachmentKey<Tracer> ATTACHMENT_KEY = AttachmentKey.create(Tracer.class);

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        ROOT_LOGGER.processingDeployment();
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        try {
            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                // SmallRye JAX-RS requires CDI. Without CDI, there's no integration needed
                ROOT_LOGGER.noCdiDeployment();
                return;
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            //We should not be here since the subsystem depends on weld capability. Just in case ...
            throw new DeploymentUnitProcessingException(ROOT_LOGGER.deploymentRequiresCapability(
                    deploymentPhaseContext.getDeploymentUnit().getName(), WELD_CAPABILITY_NAME
            ));
        }
        injectTracer(deploymentPhaseContext, support);
    }

    private String getServiceName(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if (null == jbossWebMetaData) {
            // nothing to do here
            return "";
        }
        if (jbossWebMetaData.getContextParams() != null) {
            for (ParamValueMetaData param : jbossWebMetaData.getContextParams()) {
                if (SMALLRYE_OPENTRACING_SERVICE_NAME.equals(param.getParamName())) {
                    return param.getParamValue();
                }
            }
        }
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

            ROOT_LOGGER.serviceNameDerivedFromDeploymentUnit(serviceName);
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

    private String getTracerConfiguration(DeploymentPhaseContext deploymentPhaseContext) {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if (null == jbossWebMetaData || null == jbossWebMetaData.getContextParams()) {
            return null;
        }
        for (ParamValueMetaData param : jbossWebMetaData.getContextParams()) {
            if (SMALLRYE_OPENTRACING_TRACER_CONFIGURATION.equals(param.getParamName())) {
                String value = param.getParamValue();
                if (value != null && !value.isEmpty()) {
                    return TRACER_CAPABILITY.getDynamicName(param.getParamValue());
                }
            }
        }
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (support.hasCapability(DEFAULT_TRACER_CAPABILITY_NAME)) {
            return WildFlyTracerFactory.getDefaultTracerName();
        }
        return null;
    }

    private void addContextParameter(JBossWebMetaData jbossWebMetaData, ParamValueMetaData restEasyProvider) {
        if (jbossWebMetaData == null) {
            return;
        }
        List<ParamValueMetaData> contextParams = jbossWebMetaData.getContextParams();
        if (null == contextParams) {
            contextParams = new ArrayList<>();
        }
        contextParams.add(restEasyProvider);
        jbossWebMetaData.setContextParams(contextParams);
    }

    private void injectTracer(DeploymentPhaseContext deploymentPhaseContext, CapabilityServiceSupport support) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        Tracer tracer = null;
        ClassLoader initialCl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ModuleClassLoader moduleCL = module.getClassLoader();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(moduleCL);
            //Looking for GlobalTracer
            Class globalTracerClass = moduleCL.loadClass("io.opentracing.util.GlobalTracer");
            boolean isRegistered = (Boolean) globalTracerClass.getMethod("isRegistered").invoke(null);
            if (isRegistered) {
                TracingLogger.ROOT_LOGGER.alreadyRegistered();
                return;
            }
            Class tracerResolverClass = moduleCL.loadClass("io.opentracing.contrib.tracerresolver.TracerResolver");
            tracer = (Tracer) tracerResolverClass.getMethod("resolveTracer").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ROOT_LOGGER.errorResolvingTracer(ex);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(initialCl);
        }
        // an application has the option to provide a TracerFactory
        String tracerConfigurationName = null;
        String serviceName = getServiceName(deploymentUnit);
        if (null == tracer) {
            if (null == serviceName || serviceName.isEmpty()) {
                // this should really not happen, as this is set by the deployment processor
                TracingLogger.ROOT_LOGGER.noServiceName();
                tracer = NoopTracerFactory.create();
            } else {
                tracerConfigurationName = getTracerConfiguration(deploymentPhaseContext);
                if (tracerConfigurationName != null) {
                    if (!support.hasCapability(tracerConfigurationName)) {
                        throw new DeploymentUnitProcessingException(ROOT_LOGGER.deploymentRequiresCapability(deploymentUnit.getName(), tracerConfigurationName));
                    }
                    deploymentPhaseContext.getServiceTarget().addDependency(ServiceName.parse(tracerConfigurationName));
                }
                tracer = WildFlyTracerFactory.getTracer(tracerConfigurationName, serviceName);
            }
        }
        TracingCDIExtension.registerApplicationTracer(moduleCL, tracer);
        deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(SMALLRYE_OPENTRACING_SERVICE_NAME, serviceName));
        deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(SMALLRYE_OPENTRACING_TRACER, tracer));
        deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(SMALLRYE_OPENTRACING_TRACER_MANAGED, true));
        deploymentUnit.putAttachment(ATTACHMENT_KEY, tracer);
        TracingLogger.ROOT_LOGGER.registeringTracer(tracer.getClass().getName());
        addJaxRsIntegration(deploymentUnit);
        TracingLogger.ROOT_LOGGER.initializing(tracer.toString());
        DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        if (serviceName != null) {
            if (tracerConfigurationName == null) {
                deploymentResourceSupport.getDeploymentSubsystemModel(SubsystemExtension.SUBSYSTEM_NAME).get(TRACER_CONFIGURATION_NAME).set("io.opentracing.Tracer");
                deploymentResourceSupport.getDeploymentSubsystemModel(SubsystemExtension.SUBSYSTEM_NAME).get(TRACER_CONFIGURATION).set(WildFlyTracerFactory.getModel(null, serviceName));
            } else {
                deploymentResourceSupport.getDeploymentSubsystemModel(SubsystemExtension.SUBSYSTEM_NAME).get(TRACER_CONFIGURATION_NAME).set(tracerConfigurationName);
                deploymentResourceSupport.getDeploymentSubsystemModel(SubsystemExtension.SUBSYSTEM_NAME).get(TRACER_CONFIGURATION).set(WildFlyTracerFactory.getModel(tracerConfigurationName, serviceName));
            }
        } else {
            deploymentResourceSupport.getDeploymentSubsystemModel(SubsystemExtension.SUBSYSTEM_NAME).get(TRACER_CONFIGURATION_NAME).set(tracer.getClass().getName());
        }
    }

    private void addJaxRsIntegration(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if (jbossWebMetaData == null) {
            return;
        }
        ParamValueMetaData restEasyDynamicFeature = new ParamValueMetaData();
        restEasyDynamicFeature.setParamName("resteasy.providers");
        restEasyDynamicFeature.setParamValue("org.wildfly.microprofile.opentracing.smallrye.TracerDynamicFeature");
        addContextParameter(jbossWebMetaData, restEasyDynamicFeature);

        if (jbossWebMetaData.getFilters() == null) {
            jbossWebMetaData.setFilters(new FiltersMetaData());
        }
        FilterMetaData filter = new FilterMetaData();
        filter.setFilterClass("io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter");
        filter.setAsyncSupported(true);
        filter.setFilterName("io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter");
        jbossWebMetaData.getFilters().add(filter);

        FilterMappingMetaData mapping = new FilterMappingMetaData();
        mapping.setFilterName("io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter");
        mapping.setDispatchers(Collections.singletonList(DispatcherType.REQUEST));
        mapping.setUrlPatterns(Collections.singletonList("*"));
        if (jbossWebMetaData.getFilterMappings() == null) {
            jbossWebMetaData.setFilterMappings(new ArrayList<FilterMappingMetaData>());
        }
        jbossWebMetaData.getFilterMappings().add(mapping);
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        Tracer tracer = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        if (tracer instanceof AutoCloseable) {
            try {
                ((AutoCloseable) tracer).close();
            } catch (Exception ex) {
                TracingLogger.ROOT_LOGGER.error(ex.getMessage(), ex);
            }
        }
        deploymentUnit.removeAttachment(ATTACHMENT_KEY);
    }
}
