/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentIsStoppedException;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.EJBComponentUnavailableException;
import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.component.stateless.StatelessSessionComponent;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryListener;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.security.remoting.RemoteConnection;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.ejb.client.EJBIdentifier;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EJBMethodLocator;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.server.Association;
import org.jboss.ejb.server.CancelHandle;
import org.jboss.ejb.server.ClusterTopologyListener;
import org.jboss.ejb.server.InvocationRequest;
import org.jboss.ejb.server.ListenerHandle;
import org.jboss.ejb.server.ModuleAvailabilityListener;
import org.jboss.ejb.server.Request;
import org.jboss.ejb.server.SessionOpenRequest;
import org.jboss.invocation.InterceptorContext;
import org.jboss.remoting3.Connection;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryListener;
import org.wildfly.common.annotation.NotNull;
import org.wildfly.security.auth.server.SecurityIdentity;

import javax.ejb.EJBException;
import javax.net.ssl.SSLSession;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
final class AssociationImpl implements Association, AutoCloseable {

    private static final String RETURNED_CONTEXT_DATA_KEY = "jboss.returned.keys";
    private final DeploymentRepository deploymentRepository;
    private final ClusterTopologyRegistrar clusterTopologyRegistrar;
    private final Registry<String, List<ClientMapping>> clientMappingRegistry;
    private volatile Executor executor;

    AssociationImpl(final DeploymentRepository deploymentRepository, final Registry<String, List<ClientMapping>> clientMappingRegistry) {
        this.deploymentRepository = deploymentRepository;
        this.clientMappingRegistry = clientMappingRegistry;
        this.clusterTopologyRegistrar = (clientMappingRegistry != null) ? new ClusterTopologyRegistrar(clientMappingRegistry) : null;
    }

    @Override
    public void close() {
        if (this.clusterTopologyRegistrar != null) this.clusterTopologyRegistrar.close();
    }

    @Override
    public CancelHandle receiveInvocationRequest(@NotNull final InvocationRequest invocationRequest) {

        final EJBIdentifier ejbIdentifier = invocationRequest.getEJBIdentifier();

        final String appName = ejbIdentifier.getAppName();
        final String moduleName = ejbIdentifier.getModuleName();
        final String distinctName = ejbIdentifier.getDistinctName();
        final String beanName = ejbIdentifier.getBeanName();

        final EjbDeploymentInformation ejbDeploymentInformation = findEJB(appName, moduleName, distinctName, beanName);

        if (ejbDeploymentInformation == null) {
            invocationRequest.writeNoSuchEJB();
            return CancelHandle.NULL;
        }

        final ClassLoader classLoader = ejbDeploymentInformation.getDeploymentClassLoader();

        final InvocationRequest.Resolved requestContent;
        try {
            requestContent = invocationRequest.getRequestContent(classLoader);
        } catch (IOException | ClassNotFoundException e) {
            invocationRequest.writeException(new EJBException(e));
            return CancelHandle.NULL;
        }

        final Map<String, Object> attachments = requestContent.getAttachments();

        final EJBLocator<?> ejbLocator = requestContent.getEJBLocator();

        final String viewClassName = ejbLocator.getViewType().getName();

        if (!ejbDeploymentInformation.isRemoteView(viewClassName)) {
            invocationRequest.writeWrongViewType();
            return CancelHandle.NULL;
        }

        final ComponentView componentView = ejbDeploymentInformation.getView(viewClassName);

        final Method invokedMethod = findMethod(componentView, invocationRequest.getMethodLocator());
        if (invokedMethod == null) {
            invocationRequest.writeNoSuchMethod();
            return CancelHandle.NULL;
        }

        final boolean isAsync = componentView.isAsynchronous(invokedMethod);

        final boolean oneWay = isAsync && invokedMethod.getReturnType() == void.class;

        if (oneWay) {
            // send immediate response
            updateAffinities(invocationRequest, attachments, ejbLocator, componentView);
            requestContent.writeInvocationResult(null);
        } else if(isAsync) {
            invocationRequest.writeProceedAsync();
        }

        final CancellationFlag cancellationFlag = new CancellationFlag();

        Runnable runnable = () -> {
            if (! cancellationFlag.runIfNotCancelled()) {
                if (! oneWay) invocationRequest.writeCancelResponse();
                return;
            }
            // invoke the method
            final Object result;

            // the Remoting connection that is set here is only used for legacy purposes
            Connection remotingConnection = invocationRequest.getProviderInterface(Connection.class);
            if(remotingConnection != null) {
                SecurityActions.remotingContextSetConnection(remotingConnection);
            } else {
                SecurityActions.remotingContextSetConnection(new RemoteConnection() {
                    @Override
                    public SSLSession getSslSession() {
                        return null;
                    }

                    @Override
                    public SecurityIdentity getSecurityIdentity() {
                        return invocationRequest.getSecurityIdentity();
                    }
                });
            }

            try {
                final Map<String, Object> contextDataHolder = new HashMap<>();
                result = invokeMethod(componentView, invokedMethod, invocationRequest, requestContent, cancellationFlag, contextDataHolder);
                attachments.putAll(contextDataHolder);
            } catch (EJBComponentUnavailableException ex) {
                // if the EJB is shutting down when the invocation was done, then it's as good as the EJB not being available. The client has to know about this as
                // a "no such EJB" failure so that it can retry the invocation on a different node if possible.
                EjbLogger.EJB3_INVOCATION_LOGGER.debugf("Cannot handle method invocation: %s on bean: %s due to EJB component unavailability exception. Returning a no such EJB available message back to client", invokedMethod, beanName);
                if (! oneWay) invocationRequest.writeNoSuchEJB();
                return;
            } catch (ComponentIsStoppedException ex) {
                EjbLogger.EJB3_INVOCATION_LOGGER.debugf("Cannot handle method invocation: %s on bean: %s due to EJB component stopped exception. Returning a no such EJB available message back to client", invokedMethod, beanName);
                if (! oneWay) invocationRequest.writeNoSuchEJB();
                return;
                // TODO should we write a specifc response with a specific protocol letting client know that server is suspending?
            } catch (CancellationException ex) {
                if (! oneWay) invocationRequest.writeCancelResponse();
                return;
            } catch (Exception exception) {
                if (oneWay) return;
                // write out the failure
                final Exception exceptionToWrite;
                final Throwable cause = exception.getCause();
                if (componentView.getComponent() instanceof StatefulSessionComponent && exception instanceof EJBException && cause != null) {
                    if (!(componentView.getComponent().isRemotable(cause))) {
                        // Avoid serializing the cause of the exception in case it is not remotable
                        // Client might not be able to deserialize and throw ClassNotFoundException
                        exceptionToWrite = new EJBException(exception.getLocalizedMessage());
                    } else {
                        exceptionToWrite = exception;
                    }
                } else {
                    exceptionToWrite = exception;
                }
                invocationRequest.writeException(exceptionToWrite);
                return;
            } finally {
                SecurityActions.remotingContextClear();
            }
            // invocation was successful
            if (! oneWay) try {
                updateAffinities(invocationRequest, attachments, ejbLocator, componentView);
                requestContent.writeInvocationResult(result);
            } catch (Throwable ioe) {
                EjbLogger.REMOTE_LOGGER.couldNotWriteMethodInvocation(ioe, invokedMethod, beanName, appName, moduleName, distinctName);
            }
        };
        // invoke the method and write out the response, possibly on a separate thread
        execute(invocationRequest, runnable, isAsync);
        return cancellationFlag::cancel;
    }

    private void updateAffinities(InvocationRequest invocationRequest, Map<String, Object> attachments, EJBLocator<?> ejbLocator, ComponentView componentView) {
        Affinity legacyAffinity = null;
        Affinity weakAffinity = null;
        Affinity clusterAffinity = getClusterAffinity();

        if (ejbLocator.isStateful() && componentView.getComponent() instanceof StatefulSessionComponent) {
            final StatefulSessionComponent statefulSessionComponent = (StatefulSessionComponent) componentView.getComponent();
            weakAffinity = legacyAffinity = getWeakAffinity(statefulSessionComponent, ejbLocator.asStateful());
        } else if (componentView.getComponent() instanceof StatelessSessionComponent) {
            // V3 and less used cluster affinity as a weak affinity for SLSBs
            legacyAffinity = clusterAffinity;
        }

        // Always use the cluster as the strong affinity, if there is one
        if (clusterAffinity != null) {
            invocationRequest.updateStrongAffinity(clusterAffinity);
        }

        if (weakAffinity != null && !weakAffinity.equals(Affinity.NONE)) {
            invocationRequest.updateWeakAffinity(weakAffinity);
        }

        if (legacyAffinity != null && !legacyAffinity.equals(Affinity.NONE)) {
            attachments.put(Affinity.WEAK_AFFINITY_CONTEXT_KEY, legacyAffinity);
        }
    }

    private void execute(Request request, Runnable task, final boolean isAsync) {
        if (request.getProtocol().equals("local") && ! isAsync) {
            task.run();
        } else {
            if(executor != null) {
                executor.execute(task);
            } else {
                request.getRequestExecutor().execute(task);
            }
        }
    }

    @Override
    @NotNull
    public CancelHandle receiveSessionOpenRequest(@NotNull final SessionOpenRequest sessionOpenRequest) {

        final EJBIdentifier ejbIdentifier = sessionOpenRequest.getEJBIdentifier();
        final String appName = ejbIdentifier.getAppName();
        final String moduleName = ejbIdentifier.getModuleName();
        final String beanName = ejbIdentifier.getBeanName();
        final String distinctName = ejbIdentifier.getDistinctName();

        final EjbDeploymentInformation ejbDeploymentInformation = findEJB(appName, moduleName, distinctName, beanName);
        if (ejbDeploymentInformation == null) {
            sessionOpenRequest.writeNoSuchEJB();
            return CancelHandle.NULL;
        }
        final Component component = ejbDeploymentInformation.getEjbComponent();
        component.waitForComponentStart();
        if (!(component instanceof StatefulSessionComponent)) {
            sessionOpenRequest.writeNotStateful();
            return CancelHandle.NULL;
        }
        final StatefulSessionComponent statefulSessionComponent = (StatefulSessionComponent) component;
        // generate the session id and write out the response, possibly on a separate thread
        final AtomicBoolean cancelled = new AtomicBoolean();
        Runnable runnable = () -> {
            if (cancelled.get()) {
                sessionOpenRequest.writeCancelResponse();
                return;
            }
            final SessionID sessionID;
            try {
                sessionID = statefulSessionComponent.createSessionRemote();
            }  catch (EJBComponentUnavailableException ex) {
                // if the EJB is shutting down when the invocation was done, then it's as good as the EJB not being available. The client has to know about this as
                // a "no such EJB" failure so that it can retry the invocation on a different node if possible.
                EjbLogger.EJB3_INVOCATION_LOGGER.debugf("Cannot handle session creation on bean: %s due to EJB component unavailability exception. Returning a no such EJB available message back to client", beanName);
                sessionOpenRequest.writeNoSuchEJB();
                return;
            } catch (ComponentIsStoppedException ex) {
                EjbLogger.EJB3_INVOCATION_LOGGER.debugf("Cannot handle session creation on bean: %s due to EJB component stopped exception. Returning a no such EJB available message back to client", beanName);
                sessionOpenRequest.writeNoSuchEJB();
                return;
                // TODO should we write a specifc response with a specific protocol letting client know that server is suspending?
            } catch (Exception t) {
                EjbLogger.REMOTE_LOGGER.exceptionGeneratingSessionId(t, statefulSessionComponent.getComponentName(), ejbIdentifier);
                sessionOpenRequest.writeException(t);
                return;
            }

            Affinity clusterAffinity = getClusterAffinity();
            if (clusterAffinity != null) {
                sessionOpenRequest.updateStrongAffinity(clusterAffinity);
            }

            Affinity weakAffinity = getWeakAffinity(statefulSessionComponent, sessionID);
            if (weakAffinity != null && !Affinity.NONE.equals(weakAffinity)) {
                sessionOpenRequest.updateWeakAffinity(weakAffinity);
            }

            sessionOpenRequest.convertToStateful(sessionID);
        };
        execute(sessionOpenRequest, runnable, false);
        return ignored -> cancelled.set(true);
    }

    @Override
    public ListenerHandle registerClusterTopologyListener(@NotNull final ClusterTopologyListener clusterTopologyListener) {
        return (this.clusterTopologyRegistrar != null) ? this.clusterTopologyRegistrar.registerClusterTopologyListener(clusterTopologyListener) : () -> {};
    }

    @Override
    public ListenerHandle registerModuleAvailabilityListener(@NotNull final ModuleAvailabilityListener moduleAvailabilityListener) {
        final DeploymentRepositoryListener listener = new DeploymentRepositoryListener() {
            @Override
            public void listenerAdded(final DeploymentRepository repository) {
                List<EJBModuleIdentifier> list = new ArrayList<>();
                for (DeploymentModuleIdentifier deploymentModuleIdentifier : repository.getModules().keySet()) {
                    EJBModuleIdentifier ejbModuleIdentifier = toModuleIdentifier(deploymentModuleIdentifier);
                    list.add(ejbModuleIdentifier);
                }
                moduleAvailabilityListener.moduleAvailable(list);
            }

            @Override
            public void deploymentAvailable(final DeploymentModuleIdentifier deployment, final ModuleDeployment moduleDeployment) {
                moduleAvailabilityListener.moduleAvailable(Collections.singletonList(toModuleIdentifier(deployment)));
            }

            @Override
            public void deploymentStarted(final DeploymentModuleIdentifier deployment, final ModuleDeployment moduleDeployment) {
            }

            @Override
            public void deploymentRemoved(final DeploymentModuleIdentifier deployment) {
                moduleAvailabilityListener.moduleUnavailable(Collections.singletonList(toModuleIdentifier(deployment)));
            }

            @Override
            public void deploymentSuspended(DeploymentModuleIdentifier deployment) {
                moduleAvailabilityListener.moduleUnavailable(Collections.singletonList(toModuleIdentifier(deployment)));
            }

            @Override
            public void deploymentResumed(DeploymentModuleIdentifier deployment) {
                moduleAvailabilityListener.moduleAvailable(Collections.singletonList(toModuleIdentifier(deployment)));
            }
        };
        deploymentRepository.addListener(listener);
        return () -> deploymentRepository.removeListener(listener);
    }

    static EJBModuleIdentifier toModuleIdentifier(final DeploymentModuleIdentifier identifier) {
        return new EJBModuleIdentifier(identifier.getApplicationName(), identifier.getModuleName(), identifier.getDistinctName());
    }

    private EjbDeploymentInformation findEJB(final String appName, final String moduleName, final String distinctName, final String beanName) {
        final DeploymentModuleIdentifier ejbModule = new DeploymentModuleIdentifier(appName, moduleName, distinctName);
        final Map<DeploymentModuleIdentifier, ModuleDeployment> modules = this.deploymentRepository.getStartedModules();
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        final ModuleDeployment moduleDeployment = modules.get(ejbModule);
        if (moduleDeployment == null) {
            return null;
        }
        return moduleDeployment.getEjbs().get(beanName);
    }

    private class ClusterTopologyRegistrar implements RegistryListener<String, List<ClientMapping>> {
        private final Set<ClusterTopologyListener> clusterTopologyListeners = ConcurrentHashMap.newKeySet();
        private final Registry<String, List<ClientMapping>> clientMappingRegistry;
        private final Registration listenerRegistration;

        ClusterTopologyRegistrar(Registry<String, List<ClientMapping>> clientMappingRegistry) {
            this.clientMappingRegistry = clientMappingRegistry;
            this.listenerRegistration = clientMappingRegistry.register(this);
        }

        @Override
        public void addedEntries(Map<String, List<ClientMapping>> added) {
            ClusterTopologyListener.ClusterInfo info = getClusterInfo(added);
            for (ClusterTopologyListener listener : this.clusterTopologyListeners) {// Synchronize each listener to ensure that the initial topology was set before processing new entries
                synchronized (listener) {
                    listener.clusterNewNodesAdded(info);
                }
            }
        }

        @Override
        public void updatedEntries(Map<String, List<ClientMapping>> updated) {
            this.addedEntries(updated);
        }

        @Override
        public void removedEntries(Map<String, List<ClientMapping>> removed) {
            List<ClusterTopologyListener.ClusterRemovalInfo> removals = Collections.singletonList(new ClusterTopologyListener.ClusterRemovalInfo(this.clientMappingRegistry.getGroup().getName(), new ArrayList<>(removed.keySet())));
            for (ClusterTopologyListener listener : this.clusterTopologyListeners) {// Synchronize each listener to ensure that the initial topology was set before processing removed entries
                synchronized (listener) {
                    listener.clusterNodesRemoved(removals);
                }
            }
        }

        ListenerHandle registerClusterTopologyListener(ClusterTopologyListener listener) {
            // Synchronize on the listener to ensure that the initial topology is set before processing any changes from the registry listener
            synchronized (listener) {
                this.clusterTopologyListeners.add(listener);
                listener.clusterTopology(!this.clientMappingRegistry.getGroup().isSingleton() ? Collections.singletonList(getClusterInfo(this.clientMappingRegistry.getEntries())) : Collections.emptyList());
            }
            return () -> this.clusterTopologyListeners.remove(listener);
        }

        void close() {
            this.listenerRegistration.close();
            this.clusterTopologyListeners.clear();
        }

        private ClusterTopologyListener.ClusterInfo getClusterInfo(final Map<String, List<ClientMapping>> entries) {
            final List<ClusterTopologyListener.NodeInfo> nodeInfoList = new ArrayList<>(entries.size());
            for (Map.Entry<String, List<ClientMapping>> entry : entries.entrySet()) {
                final String nodeName = entry.getKey();
                final List<ClientMapping> clientMappingList = entry.getValue();
                final List<ClusterTopologyListener.MappingInfo> mappingInfoList = new ArrayList<>(clientMappingList.size());
                for (ClientMapping clientMapping : clientMappingList) {
                    mappingInfoList.add(new ClusterTopologyListener.MappingInfo(
                        clientMapping.getDestinationAddress(),
                        clientMapping.getDestinationPort(),
                        clientMapping.getSourceNetworkAddress(),
                        clientMapping.getSourceNetworkMaskBits())
                    );
                }
                nodeInfoList.add(new ClusterTopologyListener.NodeInfo(nodeName, mappingInfoList));
            }
            return new ClusterTopologyListener.ClusterInfo(this.clientMappingRegistry.getGroup().getName(), nodeInfoList);
        }
    }


    static Object invokeMethod(final ComponentView componentView, final Method method, final InvocationRequest incomingInvocation, final InvocationRequest.Resolved content, final CancellationFlag cancellationFlag, Map<String, Object> contextDataHolder) throws Exception {
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.setParameters(content.getParameters());
        interceptorContext.setMethod(method);
        interceptorContext.putPrivateData(Component.class, componentView.getComponent());
        interceptorContext.putPrivateData(ComponentView.class, componentView);
        interceptorContext.putPrivateData(InvocationType.class, InvocationType.REMOTE);
        interceptorContext.setBlockingCaller(false);
        // setup the contextData on the (spec specified) InvocationContext
        final Map<String, Object> invocationContextData = new HashMap<String, Object>();
        interceptorContext.setContextData(invocationContextData);
        if (content.getAttachments() != null) {
            // attach the attachments which were passed from the remote client
            for (final Map.Entry<String, Object> attachment : content.getAttachments().entrySet()){
                if (attachment == null) {
                    continue;
                }
                final String key = attachment.getKey();
                final Object value = attachment.getValue();
                // these are private to JBoss EJB implementation and not meant to be visible to the
                // application, so add these attachments to the privateData of the InterceptorContext
                if (EJBClientInvocationContext.PRIVATE_ATTACHMENTS_KEY.equals(key)) {
                    final Map<?, ?> privateAttachments = (Map<?, ?>) value;
                    for (final Map.Entry<?, ?> privateAttachment : privateAttachments.entrySet()) {
                        interceptorContext.putPrivateData(privateAttachment.getKey(), privateAttachment.getValue());
                    }
                } else {
                    // add it to the InvocationContext which will be visible to the target bean and the
                    // application specific interceptors
                    invocationContextData.put(key, value);
                }
            }
        }
        // add the session id to the interceptor context, if it's a stateful ejb locator
        final EJBLocator<?> ejbLocator = content.getEJBLocator();
        if (ejbLocator.isStateful()) {
            interceptorContext.putPrivateData(SessionID.class, ejbLocator.asStateful().getSessionId());
        }
        // add transaction
        if (content.hasTransaction()) {
            interceptorContext.setTransactionSupplier(content::getTransaction);
        }
        // add security identity
        final SecurityIdentity securityIdentity = incomingInvocation.getSecurityIdentity();
        final boolean isAsync = componentView.isAsynchronous(method);
        final boolean oneWay = isAsync && method.getReturnType() == void.class;
        final boolean isSessionBean = componentView.getComponent() instanceof SessionBeanComponent;
        if (isAsync && isSessionBean) {
            if (! oneWay) {
                interceptorContext.putPrivateData(CancellationFlag.class, cancellationFlag);
            }
            final Object result = invokeWithIdentity(componentView, interceptorContext, securityIdentity);
            handleReturningContextData(contextDataHolder, interceptorContext, content);
            return result == null ? null : ((Future<?>) result).get();
        } else {
            Object result = invokeWithIdentity(componentView, interceptorContext, securityIdentity);
            handleReturningContextData(contextDataHolder, interceptorContext, content);
            return result;
        }
    }

    private static void handleReturningContextData(Map<String, Object> contextDataHolder, InterceptorContext interceptorContext, InvocationRequest.Resolved content) {
        Set<String> returnKeys = (Set<String>) content.getAttachments().get(RETURNED_CONTEXT_DATA_KEY);
        if(returnKeys == null) {
            return;
        }
        for(String key : returnKeys) {
            if(interceptorContext.getContextData().containsKey(key)) {
                contextDataHolder.put(key, interceptorContext.getContextData().get(key));
            }
        }
    }

    private static Object invokeWithIdentity(final ComponentView componentView, final InterceptorContext interceptorContext, final SecurityIdentity securityIdentity) throws Exception {
        return securityIdentity == null ? componentView.invoke(interceptorContext) : securityIdentity.runAsFunctionEx(ComponentView::invoke, componentView, interceptorContext);
    }

    private static Method findMethod(final ComponentView componentView, final EJBMethodLocator ejbMethodLocator) {
        final Set<Method> viewMethods = componentView.getViewMethods();
        for (final Method method : viewMethods) {
            if (method.getName().equals(ejbMethodLocator.getMethodName())) {
                final Class<?>[] methodParamTypes = method.getParameterTypes();
                if (methodParamTypes.length != ejbMethodLocator.getParameterCount()) {
                    continue;
                }
                boolean found = true;
                for (int i = 0; i < methodParamTypes.length; i++) {
                    if (!methodParamTypes[i].getName().equals(ejbMethodLocator.getParameterTypeName(i))) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return method;
                }
            }
        }
        return null;
    }

    private static Affinity getWeakAffinity(final StatefulSessionComponent statefulSessionComponent, final StatefulEJBLocator<?> statefulEJBLocator) {
        final SessionID sessionID = statefulEJBLocator.getSessionId();
        return getWeakAffinity(statefulSessionComponent, sessionID);
    }

    private static Affinity getWeakAffinity(final StatefulSessionComponent statefulSessionComponent, final SessionID sessionID) {
        return statefulSessionComponent.getCache().getWeakAffinity(sessionID);
    }

    private Affinity getClusterAffinity() {
        Registry<String, List<ClientMapping>> registry = this.clientMappingRegistry;
        Group group = registry != null ? registry.getGroup() : null;

        return group != null && !group.isSingleton() ? new ClusterAffinity(group.getName()) : null;
    }

    Executor getExecutor() {
        return executor;
    }

    void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
