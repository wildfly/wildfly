/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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


import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ejb.AsyncResult;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.utils.DescriptorUtils;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.interceptors.AsyncInvocationTask;
import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryListener;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.network.ClientMapping;
import org.jboss.ejb.client.AttachmentKeys;
import org.jboss.ejb.client.ClusterContext;
import org.jboss.ejb.client.ClusterNodeManager;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.ejb.client.EJBReceiverContext;
import org.jboss.ejb.client.EJBReceiverInvocationContext;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.TransactionID;
import org.jboss.ejb.client.remoting.NetworkUtil;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;
import org.jboss.marshalling.cloner.ClassLoaderClassCloner;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectCloners;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link EJBReceiver} for local same-VM invocations. This handles all invocations on remote interfaces
 * within the server JVM.
 *
 * @author Stuart Douglas
 */
public class LocalEjbReceiver extends EJBReceiver implements Service<LocalEjbReceiver>, RegistryCollector.Listener<String, List<ClientMapping>> {

    private static final Logger logger = Logger.getLogger(LocalEjbReceiver.class);
    public static final ServiceName DEFAULT_LOCAL_EJB_RECEIVER_SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("default-local-ejb-receiver-service");
    public static final ServiceName BY_VALUE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "localEjbReceiver", "value");
    public static final ServiceName BY_REFERENCE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "localEjbReceiver", "reference");

    private static final Object[] EMPTY_OBJECT_ARRAY = {};

    final List<EJBReceiverContext> contexts = new CopyOnWriteArrayList<EJBReceiverContext>();
    private final InjectedValue<DeploymentRepository> deploymentRepository = new InjectedValue<DeploymentRepository>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<RegistryCollector> clusterRegistryCollector = new InjectedValue<RegistryCollector>();
    private final Listener deploymentListener = new Listener();
    private final boolean allowPassByReference;
    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<>();
    private final InjectedValue<EJBRemoteConnectorService> ejbRemoteConnectorServiceValue = new InjectedValue<>();
    private final Set<ClusterTopologyUpdateListener> clusterTopologyUpdateListeners = Collections.synchronizedSet(new HashSet<ClusterTopologyUpdateListener>());


    public LocalEjbReceiver(final String nodeName, final boolean allowPassByReference) {
        super(nodeName);
        this.allowPassByReference = allowPassByReference;
    }

    @Override
    protected void associate(final EJBReceiverContext receiverContext) {
        this.contexts.add(receiverContext);

        final RegistryCollector<String, List<ClientMapping>> clusters = this.clusterRegistryCollector.getOptionalValue();
        if (clusters == null) {
            return;
        }
        // for each cluster update the EJB client context with the current nodes in the cluster
        for (final Registry<String, List<ClientMapping>> cluster : clusters.getRegistries()) {
            this.addClusterNodes(receiverContext.getClientContext(), cluster.getGroup().getName(), cluster.getEntries());
        }
    }

    @Override
    protected void processInvocation(final EJBClientInvocationContext invocation, final EJBReceiverInvocationContext receiverContext) throws Exception {
        final EJBLocator<?> locator = invocation.getLocator();
        final EjbDeploymentInformation ejb = findBean(locator.getAppName(), locator.getModuleName(), locator.getDistinctName(), locator.getBeanName());
        final EJBComponent ejbComponent = ejb.getEjbComponent();

        final Class<?> viewClass = invocation.getViewClass();
        final ComponentView view = ejb.getView(viewClass.getName());
        if (view == null) {
            throw EjbLogger.ROOT_LOGGER.viewNotFound(viewClass.getName(), ejb.getEjbName());
        }
        // make sure it's a remote view
        if (!ejb.isRemoteView(viewClass.getName())) {
            throw EjbLogger.ROOT_LOGGER.viewNotFound(viewClass.getName(), ejb.getEjbName());
        }
        final ClonerConfiguration paramConfig = new ClonerConfiguration();
        paramConfig.setClassCloner(new ClassLoaderClassCloner(ejb.getDeploymentClassLoader()));
        final ObjectCloner parameterCloner = createCloner(paramConfig);
        //TODO: this is not very efficient
        final Method method = view.getMethod(invocation.getInvokedMethod().getName(), DescriptorUtils.methodDescriptor(invocation.getInvokedMethod()));

        final boolean async = view.isAsynchronous(method);

        final Object[] parameters;
        if (invocation.getParameters() == null) {
            parameters = EMPTY_OBJECT_ARRAY;
        } else {
            parameters = new Object[invocation.getParameters().length];
            for (int i = 0; i < parameters.length; ++i) {
                parameters[i] = clone(method.getParameterTypes()[i], parameterCloner, invocation.getParameters()[i], allowPassByReference);
            }
        }

        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.setParameters(parameters);
        interceptorContext.setMethod(method);
        interceptorContext.setTarget(invocation.getInvokedProxy());
        // setup the context data in the InterceptorContext
        final Map<Object, Object> privateAttachments = invocation.getAttachments();
        final Map<String, Object> invocationContextData = invocation.getContextData();
        if (invocationContextData == null && privateAttachments.isEmpty()) {
            // no private or public data
            interceptorContext.setContextData(new HashMap<String, Object>());
        } else {
            final Map<String, Object> data = new HashMap<String, Object>();
            interceptorContext.setContextData(data);

            // write out public (application specific) context data
            for (Map.Entry<String, Object> entry : invocationContextData.entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
            if (!privateAttachments.isEmpty()) {
                // now write out the JBoss specific attachments under a single key and the value will be the
                // entire map of JBoss specific attachments
                data.put(EJBClientInvocationContext.PRIVATE_ATTACHMENTS_KEY, privateAttachments);
            }
            // Note: The code here is just for backward compatibility of 1.0.x version of EJB client project
            // against AS7 7.1.x releases. Discussion here https://github.com/jbossas/jboss-ejb-client/pull/11#issuecomment-6573863
            final boolean txIdAttachmentPresent = privateAttachments.containsKey(AttachmentKeys.TRANSACTION_ID_KEY);
            if (txIdAttachmentPresent) {
                // we additionally add/duplicate the transaction id under a different attachment key
                // to preserve backward compatibility. This is here just for 1.0.x backward compatibility
                data.put(TransactionID.PRIVATE_DATA_KEY, privateAttachments.get(AttachmentKeys.TRANSACTION_ID_KEY));
            }
        }
        interceptorContext.putPrivateData(Component.class, ejbComponent);
        interceptorContext.putPrivateData(ComponentView.class, view);

        if (locator instanceof StatefulEJBLocator) {
            final SessionID sessionID = ((StatefulEJBLocator<?>) locator).getSessionId();
            interceptorContext.putPrivateData(SessionID.class, sessionID);
        } else if (locator instanceof EntityEJBLocator) {
            final Object primaryKey = ((EntityEJBLocator<?>) locator).getPrimaryKey();
            interceptorContext.putPrivateData(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, primaryKey);
        }

        final ClonerConfiguration config = new ClonerConfiguration();
        config.setClassCloner(new LocalInvocationClassCloner(WildFlySecurityManager.getClassLoaderPrivileged(invocation.getInvokedProxy().getClass())));
        final ObjectCloner resultCloner = createCloner(config);
        if (async) {
            if (ejbComponent instanceof SessionBeanComponent) {
                final SessionBeanComponent component = (SessionBeanComponent) ejbComponent;
                final CancellationFlag flag = new CancellationFlag();
                final SecurityContext securityContext = SecurityContextAssociation.getSecurityContext();
                final AsyncInvocationTask task = new AsyncInvocationTask(flag) {

                    @Override
                    protected Object runInvocation() throws Exception {
                        setSecurityContextOnAssociation(securityContext);
                        try {
                            Object result = view.invoke(interceptorContext);
                            // if the result is null, there is no cloning needed
                            if(result == null) {
                                return result;
                            }
                            // WFLY-4331 - clone the result of an async task
                            if(result instanceof Future) {
                                Object asyncValue = ((Future)result).get();
                                // if the return value is null, there is no cloning needed
                                if(asyncValue == null) {
                                    return asyncValue;
                                }
                                return new AsyncResult(LocalEjbReceiver.clone(asyncValue.getClass(), resultCloner, asyncValue, allowPassByReference));
                            }
                            return LocalEjbReceiver.clone(result.getClass(), resultCloner, result, allowPassByReference);
                        } catch(ExecutionException e) {
                            // WFLY-4331 - clone the exception of an async task
                            throw ((Exception) LocalEjbReceiver.clone(e.getClass(), resultCloner, e, allowPassByReference));
                        } finally {
                            clearSecurityContextOnAssociation();
                        }
                    }
                };
                interceptorContext.putPrivateData(CancellationFlag.class, flag);
                component.getAsynchronousExecutor().submit(task);
                receiverContext.resultReady(new ImmediateResultProducer(task));
            } else {
                throw EjbLogger.ROOT_LOGGER.asyncInvocationOnlyApplicableForSessionBeans();
            }
        } else {
            final Object result;
            try {
                result = view.invoke(interceptorContext);
            } catch (Exception e) {
                //we even have to clone the exception type
                //to make sure it matches
                throw (Exception) clone(Exception.class, resultCloner, e, allowPassByReference);
            }
            //we do not marshal the return type unless we have to, the spec only says we have to
            //pass parameters by reference
            //TODO: investigate the implications of this further
            final Object clonedResult = clone(invocation.getInvokedMethod().getReturnType(), resultCloner, result, allowPassByReference);
            receiverContext.resultReady(new ImmediateResultProducer(clonedResult));
        }
    }

    private ObjectCloner createCloner(final ClonerConfiguration paramConfig) {
        ObjectCloner parameterCloner;
        if(WildFlySecurityManager.isChecking()) {
            parameterCloner = WildFlySecurityManager.doUnchecked(new PrivilegedAction<ObjectCloner>() {
                @Override
                public ObjectCloner run() {
                    return ObjectCloners.getSerializingObjectClonerFactory().createCloner(paramConfig);
                }
            });
        } else {
            parameterCloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(paramConfig);
        }
        return parameterCloner;
    }

    @Override
    protected <T> StatefulEJBLocator<T> openSession(EJBReceiverContext context, Class<T> viewType, String appName, String moduleName, String distinctName, String beanName) throws IllegalArgumentException {
        final EjbDeploymentInformation ejbInfo = findBean(appName, moduleName, distinctName, beanName);
        final EJBComponent component = ejbInfo.getEjbComponent();
        if (!(component instanceof StatefulSessionComponent)) {
            throw EjbLogger.ROOT_LOGGER.notStatefulSessionBean(beanName, appName, moduleName, distinctName);
        }
        final StatefulSessionComponent statefulComponent = (StatefulSessionComponent) component;
        final SessionID sessionID = statefulComponent.createSession();
        return new StatefulEJBLocator<T>(viewType, appName, moduleName, beanName, distinctName, sessionID, statefulComponent.getCache().getStrictAffinity(), this.getNodeName());
    }

    private static Object clone(final Class<?> target, final ObjectCloner cloner, final Object object, final boolean allowPassByReference) {
        if (object == null) {
            return null;
        }
        // don't clone primitives
        if (target.isPrimitive()) {
            return object;
        }
        if (allowPassByReference && target.isAssignableFrom(object.getClass())) {
            return object;
        }
        return clone(cloner, object);
    }

    private static Object clone(final ObjectCloner cloner, final Object object) {
        if (object == null) {
            return null;
        }

        try {
            if(WildFlySecurityManager.isChecking()) {
                return WildFlySecurityManager.doUnchecked(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws IOException, ClassNotFoundException {
                        return cloner.clone(object);
                    }
                });
            } else {
                return cloner.clone(object);
            }
        } catch (Exception e) {
            throw EjbLogger.ROOT_LOGGER.failedToMarshalEjbParameters(e);
        }
    }


    @Override
    protected boolean exists(final String appName, final String moduleName, final String distinctName, final String beanName) {
        try {
            final EjbDeploymentInformation ejbDeploymentInformation = findBean(appName, moduleName, distinctName, beanName);
            return ejbDeploymentInformation != null;
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }

    @Override
    protected int sendPrepare(EJBReceiverContext context, TransactionID transactionID) throws XAException {
        // send a XA_OK since a local receiver doesn't have to do anything more
        return XAResource.XA_OK;
    }

    @Override
    protected void sendCommit(EJBReceiverContext context, TransactionID transactionID, boolean onePhase) throws XAException {
        // no-op, since a local ejb receiver doesn't have to do anything more.
        return;
    }

    @Override
    protected void sendRollback(EJBReceiverContext context, TransactionID transactionID) throws XAException {
        // no-op, since a local ejb receiver doesn't have to do anything more.
        return;
    }

    @Override
    protected void sendForget(EJBReceiverContext context, TransactionID transactionID) throws XAException {
        // no-op, since a local ejb receiver doesn't have to do anything more.
        return;
    }

    @Override
    protected void beforeCompletion(EJBReceiverContext context, TransactionID transactionID) {
        // no-op, since a local ejb receiver doesn't have to do anything more.
        return;
    }

    private EjbDeploymentInformation findBean(final String appName, final String moduleName, final String distinctName, final String beanName) {
        final ModuleDeployment module = deploymentRepository.getValue().getModules().get(new DeploymentModuleIdentifier(appName, moduleName, distinctName));
        if (module == null) {
            throw EjbLogger.ROOT_LOGGER.unknownDeployment(appName, moduleName, distinctName);
        }
        final EjbDeploymentInformation ejbInfo = module.getEjbs().get(beanName);
        if (ejbInfo == null) {
            throw EjbLogger.ROOT_LOGGER.ejbNotFoundInDeployment(beanName, appName, moduleName, distinctName);
        }
        return ejbInfo;
    }

    @Override
    public void start(final StartContext context) {

        deploymentRepository.getValue().addListener(deploymentListener);
        // register ourselves as a listener to new cluster formations/removal
        final RegistryCollector<String, List<ClientMapping>> clusters = this.clusterRegistryCollector.getValue();
        // register for cluster formation/removal events
        clusters.addListener(this);
        // for each cluster add a listener for cluster node addition/removal events and also
        // update the EJB client context with the current nodes in the cluster
        for (final Registry<String, List<ClientMapping>> cluster : clusters.getRegistries()) {
            this.addClusterNodes(cluster.getGroup().getName(), cluster.getEntries());

            final ClusterTopologyUpdateListener clusterTopologyUpdateListener = new ClusterTopologyUpdateListener(cluster);
            cluster.addListener(clusterTopologyUpdateListener);
            // keep track of this update listener so that we cleanup properly
            this.clusterTopologyUpdateListeners.add(clusterTopologyUpdateListener);

        }
    }

    @Override
    public void stop(final StopContext context) {
        for (EJBReceiverContext ctx : contexts) {
            ctx.close();
        }
        this.contexts.clear();
        deploymentRepository.getValue().removeListener(deploymentListener);
        // remove ourselves from cluster creation/removal event notifications
        @SuppressWarnings("unchecked")
        RegistryCollector<String, List<ClientMapping>> collector = this.clusterRegistryCollector.getValue();
        collector.removeListener(this);
        // we no longer are interested in cluster topology updates, so unregister the update listener
        for (final ClusterTopologyUpdateListener clusterTopologyUpdateListener : this.clusterTopologyUpdateListeners) {
            clusterTopologyUpdateListener.unregisterListener();
        }

    }

    @Override
    public LocalEjbReceiver getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Injector<DeploymentRepository> getDeploymentRepository() {
        return deploymentRepository;
    }

    public Injector<Endpoint> getEndpointInjector() {
        return this.endpointValue;
    }

    public Injector<EJBRemoteConnectorService> getRemoteConnectorServiceInjector() {
        return this.ejbRemoteConnectorServiceValue;
    }

    @Override
    public void registryAdded(Registry<String, List<ClientMapping>> cluster) {
        final String clusterName = cluster.getGroup().getName();
        this.addClusterNodes(clusterName, cluster.getEntries());
        // Register a listener for listening to removed/added nodes from the cluster
        final ClusterTopologyUpdateListener clusterTopologyUpdateListener = new ClusterTopologyUpdateListener(cluster);
        cluster.addListener(clusterTopologyUpdateListener);
        // keep track of this update listener so that we cleanup properly
        this.clusterTopologyUpdateListeners.add(clusterTopologyUpdateListener);
    }

    @Override
    public void registryRemoved(Registry<String, List<ClientMapping>> registry) {
        // Removal of the registry (service) on one node of a cluster doesn't mean the entire
        // cluster has been removed.
        // TODO: We need a different/better hook for entire cluster removal event
        // Maybe if the cluster node count reaches 0 then send a cluster removal message?
//        final String clusterName = registry.getName();
//        for (final EJBReceiverContext receiverContext : this.contexts) {
//            receiverContext.getClientContext().removeCluster(clusterName);
//        }
    }

    @SuppressWarnings("rawtypes")
    public Injector<RegistryCollector> getClusterRegistryCollectorInjector() {
        return this.clusterRegistryCollector;
    }

    void addClusterNodes(final String clusterName, final Map<String, List<ClientMapping>> addedNodes) {
        if (addedNodes == null || addedNodes.isEmpty()) {
            return;
        }
        final List<EJBReceiverContext> receiverContexts = this.contexts;
        for (final EJBReceiverContext receiverContext : receiverContexts) {
            this.addClusterNodes(receiverContext.getClientContext(), clusterName, addedNodes);
        }
    }

    private void addClusterNodes(final EJBClientContext ejbClientContext, final String clusterName, final Map<String, List<ClientMapping>> addedNodes) {
        if (addedNodes == null || addedNodes.isEmpty()) {
            return;
        }
        final EJBRemoteConnectorService ejbRemoteConnectorService = this.ejbRemoteConnectorServiceValue.getOptionalValue();
        final Endpoint endpoint = this.endpointValue.getOptionalValue();
        if(ejbRemoteConnectorService == null || endpoint == null) {
            return;
        }
        final List<EJBRemoteConnectorService.EjbListenerAddress> listeningAddresses = ejbRemoteConnectorService.getListeningAddresses();
        final ClusterContext clusterContext = ejbClientContext.getOrCreateClusterContext(clusterName);
        // add the nodes to the cluster context
        for (Map.Entry<String, List<ClientMapping>> entry : addedNodes.entrySet()) {
            final String addedNodeName = entry.getKey();
            // if the current node is being added, then let the local receiver handle it
            if (LocalEjbReceiver.this.getNodeName().equals(addedNodeName)) {
                clusterContext.addClusterNodes(new LocalClusterNodeManager());
                continue;
            }
            // if the EJB client context is the default server level EJB client context
            // which can only handle local receiver and no remote receivers (due to lack of configurations
            // to connect to them), then skip that context
            if (this.isLocalOnlyEJBClientContext(ejbClientContext)) {
                logger.debugf("Skipping cluster node additions to EJB client context %s since it can only handle local node", ejbClientContext);
                continue;
            }
            // find a matching client mapping for our bind address
            final List<ClientMapping> clientMappings = entry.getValue();
            ClientMapping resolvedClientMapping = null;
            for (final ClientMapping clientMapping : clientMappings) {
                final InetAddress sourceNetworkAddress = clientMapping.getSourceNetworkAddress();
                final int netMask = clientMapping.getSourceNetworkMaskBits();
                for(EJBRemoteConnectorService.EjbListenerAddress binding : listeningAddresses) {
                    final boolean match = NetworkUtil.belongsToNetwork(binding.getAddress().getAddress(), sourceNetworkAddress, (byte) (netMask & 0xff));
                    if (match) {
                        resolvedClientMapping = clientMapping;
                        logger.debugf("Client mapping %s matches client address %s", clientMapping, binding.getAddress().getAddress());
                        break;
                    }
                }
                if(resolvedClientMapping != null) {
                    break;
                }
            }
            if (resolvedClientMapping == null) {
                EjbLogger.ROOT_LOGGER.cannotAddClusterNodeDueToUnresolvableClientMapping(addedNodeName, clusterName, listeningAddresses);
                continue;
            }
            final ClusterNodeManager remotingClusterNodeManager = new RemotingConnectionClusterNodeManager(clusterContext, endpoint, addedNodeName, resolvedClientMapping.getDestinationAddress(), resolvedClientMapping.getDestinationPort(), ejbRemoteConnectorService.getProtocol());
            clusterContext.addClusterNodes(remotingClusterNodeManager);
        }

    }

    /**
     * Returns true if the passed {@link EJBClientContext} can have only {@link LocalEjbReceiver}s.
     * Else returns false
     *
     * @param ejbClientContext The EJB client context.
     * @return
     */
    private boolean isLocalOnlyEJBClientContext(final EJBClientContext ejbClientContext) {
        final EJBClientConfiguration clientConfiguration = ejbClientContext.getEJBClientConfiguration();
        if (clientConfiguration instanceof DefaultEjbClientContextService.LocalOnlyEjbClientConfiguration) {
            return true;
        }
        return false;
    }

    private static class ImmediateResultProducer implements EJBReceiverInvocationContext.ResultProducer {

        private final Object clonedResult;

        public ImmediateResultProducer(final Object clonedResult) {
            this.clonedResult = clonedResult;
        }

        @Override
        public Object getResult() {
            return clonedResult;
        }

        @Override
        public void discardResult() {

        }
    }

    /**
     * Listener that updates the accessible set of modules
     */
    class Listener implements DeploymentRepositoryListener {

        @Override
        public void listenerAdded(final DeploymentRepository repository) {
            for (Map.Entry<DeploymentModuleIdentifier, ModuleDeployment> entry : repository.getModules().entrySet()) {
                final DeploymentModuleIdentifier module = entry.getKey();
                LocalEjbReceiver.this.registerModule(module.getApplicationName(), module.getModuleName(), module.getDistinctName());
            }
        }

        @Override
        public void deploymentAvailable(final DeploymentModuleIdentifier deployment, final ModuleDeployment moduleDeployment) {
            LocalEjbReceiver.this.registerModule(deployment.getApplicationName(), deployment.getModuleName(), deployment.getDistinctName());
        }

        @Override
        public void deploymentStarted(DeploymentModuleIdentifier deployment, ModuleDeployment moduleDeployment) {
        }

        @Override
        public void deploymentRemoved(final DeploymentModuleIdentifier deployment) {
            LocalEjbReceiver.this.deregisterModule(deployment.getApplicationName(), deployment.getModuleName(), deployment.getDistinctName());
        }
    }

    class LocalClusterNodeManager implements ClusterNodeManager {

        @Override
        public String getNodeName() {
            return LocalEjbReceiver.this.getNodeName();
        }

        @Override
        public EJBReceiver getEJBReceiver() {
            return LocalEjbReceiver.this;
        }
    }


    private static void setSecurityContextOnAssociation(final SecurityContext sc) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                SecurityContextAssociation.setSecurityContext(sc);
                return null;
            }
        });
    }

    private static void clearSecurityContextOnAssociation() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                SecurityContextAssociation.clearSecurityContext();
                return null;
            }
        });
    }

    private class ClusterTopologyUpdateListener implements Registry.Listener<String, List<ClientMapping>> {

        private final Registry<String, List<ClientMapping>> cluster;

        ClusterTopologyUpdateListener(final Registry<String, List<ClientMapping>> cluster) {
            this.cluster = cluster;
        }

        @Override
        public void addedEntries(Map<String, List<ClientMapping>> addedNodes) {
            LocalEjbReceiver.this.addClusterNodes(this.cluster.getGroup().getName(), addedNodes);
        }

        @Override
        public void updatedEntries(Map<String, List<ClientMapping>> updated) {
            // We don't support client mapping updates just yet
        }

        @Override
        public void removedEntries(Map<String, List<ClientMapping>> removedNodes) {
            final List<EJBReceiverContext> receiverContexts = LocalEjbReceiver.this.contexts;
            for (final EJBReceiverContext receiverContext : receiverContexts) {
                final ClusterContext clusterContext = receiverContext.getClientContext().getClusterContext(this.cluster.getGroup().getName());
                if (clusterContext == null) {
                    continue;
                }
                // remove the nodes from the cluster context
                for (final String removedNodeName : removedNodes.keySet()) {
                    clusterContext.removeClusterNode(removedNodeName);
                }
            }
        }

        void unregisterListener() {
            this.cluster.removeListener(this);
        }
    }

    @Override
    protected void disassociate(EJBReceiverContext receiverContext) {
        this.contexts.remove(receiverContext);

        final RegistryCollector<String, List<ClientMapping>> clusters = this.clusterRegistryCollector.getOptionalValue();
        if (clusters == null) {
            return;
        }

        // for each cluster update the EJB client context with the current nodes in the cluster
        for (final Registry<String, List<ClientMapping>> cluster : clusters.getRegistries()) {
            this.removeClusterNodes(receiverContext.getClientContext(), cluster.getGroup().getName(), cluster.getEntries());
        }
    }

    private void removeClusterNodes(final EJBClientContext ejbClientContext, final String clusterName, final Map<String, List<ClientMapping>> removedNodes) {
        if (removedNodes == null || removedNodes.isEmpty()) {
            return;
        }
        final EJBRemoteConnectorService ejbRemoteConnectorService = this.ejbRemoteConnectorServiceValue.getOptionalValue();
        final Endpoint endpoint = this.endpointValue.getOptionalValue();
        if(ejbRemoteConnectorService == null || endpoint == null) {
            return;
        }

        final ClusterContext clusterContext = ejbClientContext.getClusterContext(clusterName);
        // remove the nodes from the cluster context
        for (Map.Entry<String, List<ClientMapping>> entry : removedNodes.entrySet()) {
            final String removedNodeName = entry.getKey();
            // if the current node is being removed, then let the local receiver handle it
            if (LocalEjbReceiver.this.getNodeName().equals(removedNodeName)) {
                clusterContext.removeClusterNode(removedNodeName);
                continue;
            }
            // if the EJB client context is the default server level EJB client context
            // which can only handle local receiver and no remote receivers (due to lack of configurations
            // to connect to them), then skip that context
            if (this.isLocalOnlyEJBClientContext(ejbClientContext)) {
                logger.debug("Skipping cluster node removal to EJB client context " + ejbClientContext + " since it can only handle local node");
                continue;
            }
            clusterContext.removeClusterNode(removedNodeName);
        }
    }
}
