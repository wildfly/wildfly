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

import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.clustering.registry.RegistryCollector;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.utils.DescriptorUtils;
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
import org.jboss.ejb.client.ClusterContext;
import org.jboss.ejb.client.ClusterNodeManager;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.ejb.client.EJBReceiverContext;
import org.jboss.ejb.client.EJBReceiverInvocationContext;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.invocation.InterceptorContext;
import org.jboss.marshalling.cloner.ClassLoaderClassCloner;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectCloners;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link EJBReceiver} for local same-VM invocations. This handles all invocations on remote interfaces
 * withing the server JVM.
 *
 * @author Stuart Douglas
 */
public class LocalEjbReceiver extends EJBReceiver implements Service<LocalEjbReceiver>, RegistryCollector.Listener<String, List<ClientMapping>> {

    public static final ServiceName BY_VALUE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "localEjbReceiver", "value");
    public static final ServiceName BY_REFERENCE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "localEjbReceiver", "reference");

    private static final Object[] EMPTY_OBJECT_ARRAY = {};

    private final List<EJBReceiverContext> contexts = new CopyOnWriteArrayList<EJBReceiverContext>();
    private final InjectedValue<DeploymentRepository> deploymentRepository = new InjectedValue<DeploymentRepository>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<RegistryCollector> clusterRegistryCollector = new InjectedValue<RegistryCollector>();
    private final Listener deploymentListener = new Listener();
    private final boolean allowPassByReference;

    public LocalEjbReceiver(final String nodeName, final boolean allowPassByReference) {
        super(nodeName);
        this.allowPassByReference = allowPassByReference;
    }


    @Override
    protected void associate(final EJBReceiverContext context) {
        this.contexts.add(context);
    }

    @Override
    protected void processInvocation(final EJBClientInvocationContext invocation, final EJBReceiverInvocationContext receiverContext) throws Exception {
        final EJBLocator locator = invocation.getLocator();
        final EjbDeploymentInformation ejb = findBean(locator.getAppName(), locator.getModuleName(), locator.getDistinctName(), locator.getBeanName());
        final EJBComponent ejbComponent = ejb.getEjbComponent();

        final Class<?> viewClass = invocation.getViewClass();
        final ComponentView view = ejb.getView(viewClass.getName());
        if (view == null) {
            throw new RuntimeException("Could not find view " + viewClass + " for ejb " + ejb.getEjbName());
        }

        final ClonerConfiguration paramConfig = new ClonerConfiguration();
        paramConfig.setClassCloner(new ClassLoaderClassCloner(ejb.getDeploymentClassLoader()));
        final ObjectCloner parameterCloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(paramConfig);

        //TODO: this is not very efficent
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

        final InterceptorContext context = new InterceptorContext();
        context.setParameters(parameters);
        context.setMethod(method);
        context.setTarget(invocation.getInvokedProxy());
        context.setContextData(new HashMap<String, Object>());
        context.putPrivateData(Component.class, ejbComponent);
        context.putPrivateData(ComponentView.class, view);


        if (locator instanceof StatefulEJBLocator) {
            final SessionID sessionID = ((StatefulEJBLocator) locator).getSessionId();
            context.putPrivateData(SessionID.class, sessionID);
        } else if (locator instanceof EntityEJBLocator) {
            final Object primaryKey = ((EntityEJBLocator) locator).getPrimaryKey();
            context.putPrivateData(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, primaryKey);
        }

        final ClonerConfiguration config = new ClonerConfiguration();
        config.setClassCloner(new ClassLoaderClassCloner(invocation.getInvokedProxy().getClass().getClassLoader()));
        final ObjectCloner resultCloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(config);
        if (async) {
            if (ejbComponent instanceof SessionBeanComponent) {
                final SessionBeanComponent component = (SessionBeanComponent) ejbComponent;
                final CancellationFlag flag = new CancellationFlag();
                final AsyncInvocationTask task = new AsyncInvocationTask(flag) {

                    @Override
                    protected Object runInvocation() throws Exception {
                        return view.invoke(context);
                    }
                };
                context.putPrivateData(CancellationFlag.class, flag);
                component.getAsynchronousExecutor().submit(task);
                //TODO: we do not clone the result of an async task
                //TODO: we do not clone the exception of an async task
                receiverContext.resultReady(new ImmediateResultProducer(task));
            } else {
                throw new RuntimeException("Cannot perform asynchronous local invocation for component that is not a session bean");
            }
        } else {
            final Object result;
            try {
                result = view.invoke(context);
            } catch (Exception e) {
                //we even have to clone the exception type
                //to make sure it matches
                throw (Exception) clone(resultCloner, e);
            }
            //we do not marshal the return type unless we have to, the spec only says we have to
            //pass parameters by reference
            //TODO: investigate the implications of this further
            final Object clonedResult = clone(invocation.getInvokedMethod().getReturnType(), resultCloner, result, allowPassByReference);
            receiverContext.resultReady(new ImmediateResultProducer(clonedResult));
        }
    }

    @Override
    protected <T> StatefulEJBLocator<T> openSession(EJBReceiverContext context, Class<T> viewType, String appName, String moduleName, String distinctName, String beanName) throws IllegalArgumentException {
        final EjbDeploymentInformation ejbInfo = findBean(appName, moduleName, distinctName, beanName);
        final EJBComponent component = ejbInfo.getEjbComponent();
        if (!(component instanceof StatefulSessionComponent)) {
            throw new IllegalArgumentException("EJB " + beanName + " is not a Stateful Session bean in app: " + appName + " module: " + moduleName + " distinct name:" + distinctName);
        }
        final StatefulSessionComponent statefulComponent = (StatefulSessionComponent) component;
        final SessionID sessionID = statefulComponent.createSession();
        return new StatefulEJBLocator<T>(viewType, appName, moduleName, beanName, distinctName, sessionID, statefulComponent.getCache().getStrictAffinity());
    }

    private Object clone(final Class<?> target, final ObjectCloner cloner, final Object object, final boolean allowPassByReference) {
        if (object == null) {
            return null;
        }

        if (allowPassByReference && target.isAssignableFrom(object.getClass())) {
            return object;
        }
        return clone(cloner, object);
    }

    private Object clone(final ObjectCloner cloner, final Object object) {
        if (object == null) {
            return null;
        }

        try {
            return cloner.clone(object);
        } catch (IOException e) {
            throw new RuntimeException("IOException marshaling EJB parameters", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("ClassNotFoundException marshaling EJB parameters", e);
        }
    }


    @Override
    protected void verify(final String appName, final String moduleName, final String distinctName, final String beanName) throws Exception {
        findBean(appName, moduleName, distinctName, beanName);
    }

    private EjbDeploymentInformation findBean(final String appName, final String moduleName, final String distinctName, final String beanName) {
        final ModuleDeployment module = deploymentRepository.getValue().getModules().get(new DeploymentModuleIdentifier(appName, moduleName, distinctName));
        if (module == null) {
            throw new IllegalArgumentException("Could not find module app: " + appName + " module: " + moduleName + " distinct name:" + distinctName);
        }
        final EjbDeploymentInformation ejbInfo = module.getEjbs().get(beanName);
        if (ejbInfo == null) {
            throw new IllegalArgumentException("Could not find ejb " + beanName + " in app: " + appName + " module: " + moduleName + " distinct name:" + distinctName);
        }
        return ejbInfo;
    }

    @Override
    public void start(final StartContext context) throws StartException {

        deploymentRepository.getValue().addListener(deploymentListener);
        // register ourselves as a listener to new cluster formations/removal
        @SuppressWarnings("unchecked")
        RegistryCollector<String, List<ClientMapping>> collector = this.clusterRegistryCollector.getValue();
        collector.addListener(this);
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
    }

    @Override
    public LocalEjbReceiver getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepository() {
        return deploymentRepository;
    }

    @Override
    public void registryAdded(Registry<String, List<ClientMapping>> registry) {
        final String clusterName = registry.getName();
        for (final EJBReceiverContext receiverContext : this.contexts) {
            final ClusterContext clusterContext = receiverContext.getClientContext().getOrCreateClusterContext(clusterName);
            // TODO: we currently don't take into account any nodes in the cluster for Local EJB receiver.
            // We just register the current local receiver for the cluster context which effectively means that
            // any invocations via the LocalEJBReceiver for any clusters will be pinned to the current local node.
            // We need to decide whether we want to create remote ejb receiver(s) for the cluster nodes, out of this
            // local receiver
            clusterContext.addClusterNode(this.getNodeName(), new LocalClusterNodeManager());
        }
        // TODO: We also should register a listener for listening to removed/added nodes from the cluster
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

    private static class ImmediateResultProducer implements EJBReceiverInvocationContext.ResultProducer {

        private final Object clonedResult;

        public ImmediateResultProducer(final Object clonedResult) {
            this.clonedResult = clonedResult;
        }

        @Override
        public Object getResult() throws Exception {
            return clonedResult;
        }

        @Override
        public void discardResult() {

        }
    }

    /**
     * Listener that updates the accessible set of modules
     */
    private class Listener implements DeploymentRepositoryListener {

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
        public void deploymentRemoved(final DeploymentModuleIdentifier deployment) {
            LocalEjbReceiver.this.deregisterModule(deployment.getApplicationName(), deployment.getModuleName(), deployment.getDistinctName());
        }
    }

    private class LocalClusterNodeManager implements ClusterNodeManager {

        @Override
        public String getNodeName() {
            return LocalEjbReceiver.this.getNodeName();
        }

        @Override
        public EJBReceiver getEJBReceiver() {
            return LocalEjbReceiver.this;
        }
    }
}
