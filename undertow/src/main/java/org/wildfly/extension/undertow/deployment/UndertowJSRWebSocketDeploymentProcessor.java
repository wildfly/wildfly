/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import io.undertow.websockets.jsr.JsrWebSocketLogger;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import javax.websocket.ClientEndpoint;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            return;
        }

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(module.getClassLoader());


            WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
            if (metaData == null || metaData.getMergedJBossWebMetaData() == null) {
                return;
            }
            if(!metaData.getMergedJBossWebMetaData().isEnableWebSockets()) {
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
                            Class<?> moduleClass = ClassLoadingUtils.loadClass(clazz.name().toString(), module);
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
                            Class<?> moduleClass = ClassLoadingUtils.loadClass(clazz.name().toString(), module);
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
                        Class<?> moduleClass = ClassLoadingUtils.loadClass(clazz.name().toString(), module);
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
                        Class<?> moduleClass = ClassLoadingUtils.loadClass(clazz.name().toString(), module);
                        if (!Modifier.isAbstract(moduleClass.getModifiers())) {
                            endpoints.add((Class) moduleClass);
                        }
                    } catch (ClassNotFoundException e) {
                        UndertowLogger.ROOT_LOGGER.couldNotLoadWebSocketConfig(clazz.name().toString(), e);
                    }
                }
            }

            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();

            doDeployment(deploymentUnit, webSocketDeploymentInfo, annotatedEndpoints, config, endpoints);

            installWebsockets(phaseContext, webSocketDeploymentInfo);

        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }

    }

    private void installWebsockets(final DeploymentPhaseContext phaseContext, final WebSocketDeploymentInfo webSocketDeploymentInfo) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        deploymentUnit.putAttachment(UndertowAttachments.WEB_SOCKET_DEPLOYMENT_INFO, webSocketDeploymentInfo);
    }

    private void doDeployment(DeploymentUnit deploymentUnit, final WebSocketDeploymentInfo container, final Set<Class<?>> annotatedEndpoints, final Set<Class<? extends ServerApplicationConfig>> serverApplicationConfigClasses, final Set<Class<? extends Endpoint>> endpoints) throws DeploymentUnitProcessingException {

        Set<Class<? extends Endpoint>> allScannedEndpointImplementations = new HashSet<>(endpoints);
        Set<Class<?>> allScannedAnnotatedEndpoints = new HashSet<>(annotatedEndpoints);
        Set<Class<?>> newAnnotatatedEndpoints = new HashSet<>();
        Set<ServerEndpointConfig> serverEndpointConfigurations = new HashSet<>();

        final Set<ServerApplicationConfig> configInstances = new HashSet<>();
        for (Class<? extends ServerApplicationConfig> clazz : serverApplicationConfigClasses) {
            try {
                configInstances.add(clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                JsrWebSocketLogger.ROOT_LOGGER.couldNotInitializeConfiguration(clazz, e);
            }
        }


        if (!configInstances.isEmpty()) {
            for (ServerApplicationConfig config : configInstances) {
                Set<Class<?>> returnedEndpoints = config.getAnnotatedEndpointClasses(allScannedAnnotatedEndpoints);
                if (returnedEndpoints != null) {
                    newAnnotatatedEndpoints.addAll(returnedEndpoints);
                }
                Set<ServerEndpointConfig> endpointConfigs = config.getEndpointConfigs(allScannedEndpointImplementations);
                if (endpointConfigs != null) {
                    serverEndpointConfigurations.addAll(endpointConfigs);
                }
            }
        } else {
            newAnnotatatedEndpoints.addAll(allScannedAnnotatedEndpoints);
        }

        //annotated endpoints first
        for (Class<?> endpoint : newAnnotatatedEndpoints) {
            if(endpoint != null ) {
                container.addEndpoint(endpoint);
                ServerEndpoint annotation = endpoint.getAnnotation(ServerEndpoint.class);
                if (annotation != null) {
                    String path = annotation.value();
                    addManagementWebsocket(deploymentUnit, endpoint, path);
                }
            }
        }

        for (final ServerEndpointConfig endpoint : serverEndpointConfigurations) {
            if(endpoint != null) {
                container.addEndpoint(endpoint);
                addManagementWebsocket(deploymentUnit, endpoint.getEndpointClass(), endpoint.getPath());
            }
        }
    }

    void addManagementWebsocket(final DeploymentUnit unit, Class endpoint, String path) {
        try {
            final DeploymentResourceSupport deploymentResourceSupport = unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
            //websockets don't have the concept of a name, however the path much be unique
            final ModelNode node = deploymentResourceSupport.getDeploymentSubModel(UndertowExtension.SUBSYSTEM_NAME, PathElement.pathElement("websocket", path));
            node.get("endpoint-class").set(endpoint.getName());
            node.get("path").set(path);
        } catch (Exception e) {
            UndertowLogger.ROOT_LOGGER.failedToRegisterWebsocket(endpoint, path, e);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
