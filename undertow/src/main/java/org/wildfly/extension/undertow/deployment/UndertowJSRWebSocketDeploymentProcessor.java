/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.websocket.ClientEndpoint;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.websockets.jsr.JsrWebSocketFilter;
import io.undertow.websockets.jsr.JsrWebSocketLogger;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import org.jboss.as.ee.component.EEModuleDescription;
import org.wildfly.extension.io.IOServices;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.wildfly.extension.undertow.UndertowLogger;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.xnio.Pool;
import org.xnio.XnioWorker;

/**
 * Deployment processor for native JSR-356 websockets
 * <p/>
 *
 * @author Stuart Douglas
 */
public class UndertowJSRWebSocketDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName SERVER_ENDPOINT = DotName.createSimple(ServerEndpoint.class.getName());
    private static final DotName CLIENT_ENDPOINT = DotName.createSimple(ClientEndpoint.class.getName());
    private static final DotName SERVER_APPLICATION_CONFIG = DotName.createSimple(ServerApplicationConfig.class.getName());
    private static final DotName ENDPOINT = DotName.createSimple(Endpoint.class.getName());
    public static final String FILTER_NAME = "Undertow Web Socket Filter";


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final DeploymentClassIndex classIndex = deploymentUnit.getAttachment(Attachments.CLASS_INDEX);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if(module == null) {
            return;
        }

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(module.getClassLoader());


            WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
            if (metaData == null) {
                return;
            }

            final Set<Class<?>> annotatedEndpoints = new HashSet<>();
            final Set<Class<? extends Endpoint>> endpoints = new HashSet<>();
            final Set<Class<? extends ServerApplicationConfig>> config = new HashSet<>();

            final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

            final List<AnnotationInstance> serverEndpoints = index.getAnnotations(SERVER_ENDPOINT);
            if (serverEndpoints != null) {
                for (AnnotationInstance endpoint : serverEndpoints) {
                    if (endpoint.target() instanceof ClassInfo) {
                        ClassInfo clazz = (ClassInfo) endpoint.target();
                        try {
                            Class<?> moduleClass = classIndex.classIndex(clazz.name().toString()).getModuleClass();
                            if (!Modifier.isAbstract(moduleClass.getModifiers())) {
                                annotatedEndpoints.add(moduleClass);
                            }
                        } catch (ClassNotFoundException e) {
                            UndertowLogger.ROOT_LOGGER.couldNotLoadWebSocketEndpoint(clazz.name().toString(), e);
                        }
                    }
                }
            }

            final List<AnnotationInstance> clientEndpoints = index.getAnnotations(CLIENT_ENDPOINT);
            if (clientEndpoints != null) {
                for (AnnotationInstance endpoint : clientEndpoints) {
                    if (endpoint.target() instanceof ClassInfo) {
                        ClassInfo clazz = (ClassInfo) endpoint.target();
                        try {
                            Class<?> moduleClass = classIndex.classIndex(clazz.name().toString()).getModuleClass();
                            if (!Modifier.isAbstract(moduleClass.getModifiers())) {
                                annotatedEndpoints.add(moduleClass);
                            }
                        } catch (ClassNotFoundException e) {
                            UndertowLogger.ROOT_LOGGER.couldNotLoadWebSocketEndpoint(clazz.name().toString(), e);
                        }
                    }
                }
            }

            final Set<ClassInfo> subclasses = index.getAllKnownImplementors(SERVER_APPLICATION_CONFIG);

            if (subclasses != null) {
                for (final ClassInfo clazz : subclasses) {
                    try {
                        Class<?> moduleClass = classIndex.classIndex(clazz.name().toString()).getModuleClass();
                        if (!Modifier.isAbstract(moduleClass.getModifiers())) {
                            config.add((Class) moduleClass);
                        }
                    } catch (ClassNotFoundException e) {
                        UndertowLogger.ROOT_LOGGER.couldNotLoadWebSocketConfig(clazz.name().toString(), e);
                    }
                }
            }

            final Set<ClassInfo> epClasses = index.getAllKnownSubclasses(ENDPOINT);

            if (epClasses != null) {
                for (final ClassInfo clazz : epClasses) {
                    try {
                        Class<?> moduleClass = classIndex.classIndex(clazz.name().toString()).getModuleClass();
                        if (!Modifier.isAbstract(moduleClass.getModifiers())) {
                            endpoints.add((Class) moduleClass);
                        }
                    } catch (ClassNotFoundException e) {
                        UndertowLogger.ROOT_LOGGER.couldNotLoadWebSocketConfig(clazz.name().toString(), e);
                    }
                }
            }

            ServerWebSocketContainer container = new ServerWebSocketContainer(DefaultClassIntrospector.INSTANCE); //TODO: fix

            doDeployment(container, annotatedEndpoints, config, endpoints);

            installWebsockets(phaseContext, metaData, container);

        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }

    }

    private void installWebsockets(final DeploymentPhaseContext phaseContext, final WarMetaData metaData, final ServerWebSocketContainer container) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        FiltersMetaData filters = metaData.getMergedJBossWebMetaData().getFilters();
        if (filters == null) {
            metaData.getMergedJBossWebMetaData().setFilters(filters = new FiltersMetaData());
        }
        FilterMetaData filterMetaData = new FilterMetaData();
        filterMetaData.setAsyncSupported(true);
        filterMetaData.setFilterClass(JsrWebSocketFilter.class.getName());
        filterMetaData.setName(FILTER_NAME);
        filters.add(filterMetaData);

        List<FilterMappingMetaData> mappings = metaData.getMergedJBossWebMetaData().getFilterMappings();
        if (mappings == null) {
            metaData.getMergedJBossWebMetaData().setFilterMappings(mappings = new ArrayList<>());
        }
        FilterMappingMetaData mapping = new FilterMappingMetaData();
        mapping.setFilterName(FILTER_NAME);
        mapping.setDispatchers(Collections.singletonList(org.jboss.metadata.web.spec.DispatcherType.REQUEST));
        mapping.setUrlPatterns(Collections.singletonList("/*"));
        mappings.add(mapping);

        deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(ServerContainer.class.getName(), container));

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WebSocketContainerService.SERVICE_NAME);
        WebSocketContainerService service = new WebSocketContainerService(container);
        phaseContext.getServiceTarget().addService(serviceName, service)
                .addDependency(IOServices.WORKER.append("default"), XnioWorker.class, service.getXnioWorker()) //TODO: make this configurable
                .addDependency(IOServices.BUFFER_POOL.append("default"), Pool.class, service.getInjectedBuffer())
                .install();

        //bind the container to JNDI to make it availble for resource injection
        //this is not request by the spec, but is an convenient extension
        final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getApplicationName(), moduleDescription.getModuleName());
        bindJndiServices(deploymentUnit, phaseContext.getServiceTarget(), moduleContextServiceName, serviceName);

        deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, serviceName);
    }

    private void bindJndiServices(final DeploymentUnit deploymentUnit, final ServiceTarget serviceTarget, final ServiceName contextServiceName, final ServiceName serviceName) {
        final ServiceName bindingServiceName = contextServiceName.append("ServerContainer");
        final BinderService binderService = new BinderService("ServerContainer");
        serviceTarget.addService(bindingServiceName, binderService)
                .addDependency(serviceName, ServerWebSocketContainer.class, new ManagedReferenceInjector<ServerWebSocketContainer>(binderService.getManagedObjectInjector()))
                .addDependency(contextServiceName, ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .install();
        deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES, bindingServiceName);
    }

    private void doDeployment(final ServerWebSocketContainer container, final Set<Class<?>> annotatedEndpoints, final Set<Class<? extends ServerApplicationConfig>> serverApplicationConfigClasses, final Set<Class<? extends Endpoint>> endpoints) throws DeploymentUnitProcessingException {

        Set<Class<? extends Endpoint>> allScannedEndpointImplementations = new HashSet<>(endpoints);
        Set<Class<?>> allAnnotatedEndpoints = new HashSet<>(annotatedEndpoints);
        Set<ServerEndpointConfig> serverEndpointConfigurations = new HashSet<>();

        final Set<ServerApplicationConfig> configInstances = new HashSet<>();
        for (Class<? extends ServerApplicationConfig> clazz : serverApplicationConfigClasses) {
            try {
                configInstances.add(clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                JsrWebSocketLogger.ROOT_LOGGER.couldNotInitializeConfiguration(clazz, e);
            }
        }


        for (ServerApplicationConfig config : configInstances) {
            allAnnotatedEndpoints = config.getAnnotatedEndpointClasses(allAnnotatedEndpoints);
            Set<ServerEndpointConfig> endpointConfigs = config.getEndpointConfigs(allScannedEndpointImplementations);
            if(endpointConfigs != null) {
                serverEndpointConfigurations.addAll(endpointConfigs);
            }
        }

        //ok, now we have our endpoints, lets deploy them
        try {
            //annotated endpoints first
            for (Class<?> endpoint : allAnnotatedEndpoints) {
                container.addEndpoint(endpoint);
            }

            for (final ServerEndpointConfig endpoint : serverEndpointConfigurations) {
                container.addEndpoint(endpoint);
            }
        } catch (DeploymentException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
