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
import static org.wildfly.extension.microprofile.opentracing.TracingExtensionLogger.ROOT_LOGGER;
import static org.wildfly.microprofile.opentracing.smallrye.TracerConstants.SMALLRYE_OPENTRACING_SERVICE_NAME;
import static org.wildfly.microprofile.opentracing.smallrye.TracerConstants.SMALLRYE_OPENTRACING_TRACER;
import static org.wildfly.microprofile.opentracing.smallrye.TracerConstants.SMALLRYE_OPENTRACING_TRACER_MANAGED;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import java.lang.reflect.InvocationTargetException;
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
import org.wildfly.microprofile.opentracing.smallrye.TracingLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;

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
        setServiceName(deploymentUnit);
        injectTracer(deploymentPhaseContext, support);
    }

    private void setServiceName(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if (null == jbossWebMetaData) {
            // nothing to do here
            return;
        }
        String serviceName = getServiceName(deploymentUnit);
        ParamValueMetaData serviceNameContextParameter = new ParamValueMetaData();
        serviceNameContextParameter.setParamName(SMALLRYE_OPENTRACING_SERVICE_NAME);
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

            ROOT_LOGGER.serviceNameDerivedFromDeploymentUnit(serviceName);
        }

        return serviceName;
    }

    private void injectTracer(DeploymentPhaseContext deploymentPhaseContext, CapabilityServiceSupport support) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        Tracer tracer = null;
        ClassLoader initialCl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            final ModuleClassLoader moduleCL = module.getClassLoader();
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(moduleCL);
            //Looking for GlobalTracer
            Class globalTracerClass = moduleCL.loadClass("io.opentracing.util.GlobalTracer");
            boolean isRegistered = (Boolean) globalTracerClass.getMethod("isRegistered").invoke(null);
            if(isRegistered) {
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
        String serviceName = getServiceName(deploymentUnit);
        if (null == tracer) {
            if (null == serviceName || serviceName.isEmpty()) {
                // this should really not happen, as this is set by the deployment processor
                TracingLogger.ROOT_LOGGER.noServiceName();
                tracer = NoopTracerFactory.create();
            } else {
                tracer = Configuration.fromEnv(serviceName).getTracerBuilder().withManualShutdown().build();
            }
        }
        deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(SMALLRYE_OPENTRACING_SERVICE_NAME, serviceName));
        deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(SMALLRYE_OPENTRACING_TRACER, tracer));
        deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(SMALLRYE_OPENTRACING_TRACER_MANAGED, true));
        deploymentUnit.putAttachment(ATTACHMENT_KEY, tracer);
        TracingLogger.ROOT_LOGGER.registeringTracer(tracer.getClass().getName());
        addJaxRsIntegration(deploymentUnit);
        TracingLogger.ROOT_LOGGER.initializing(tracer.toString());
    }

    private void addJaxRsIntegration(DeploymentUnit deploymentUnit) {
        JBossWebMetaData jbossWebMetaData = getJBossWebMetaData(deploymentUnit);
        if(jbossWebMetaData == null) {
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
