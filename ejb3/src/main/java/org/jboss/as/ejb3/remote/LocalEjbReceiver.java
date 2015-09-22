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


import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.as.ee.utils.DescriptorUtils;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.interceptors.AsyncInvocationTask;
import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryListener;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.AttachmentKey;
import org.jboss.ejb.client.AttachmentKeys;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.ejb.client.EJBReceiverInvocationContext;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.TransactionID;
import org.jboss.invocation.InterceptorContext;
import org.jboss.marshalling.cloner.ClassLoaderClassCloner;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectCloners;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.discovery.AttributeValue;
import org.wildfly.discovery.FilterSpec;
import org.wildfly.discovery.ServiceType;
import org.wildfly.discovery.ServiceURL;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryRequest;
import org.wildfly.discovery.spi.DiscoveryResult;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.ejb.AsyncResult;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * {@link EJBReceiver} for local same-VM invocations. This handles all invocations on remote interfaces
 * within the server JVM.
 *
 * @author Stuart Douglas
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class LocalEjbReceiver extends EJBReceiver implements DiscoveryProvider {

    private static final Object[] EMPTY_OBJECT_ARRAY = {};

    private final Listener deploymentListener = new Listener();

    private final DeploymentRepository deploymentRepository;

    private final boolean allowPassByReference;

    private final Map<DeploymentModuleIdentifier, ServiceURL> accessibleModules = Collections.synchronizedMap(new LinkedHashMap<>());

    public LocalEjbReceiver(final boolean allowPassByReference, final DeploymentRepository deploymentRepository) {
        this.allowPassByReference = allowPassByReference;
        this.deploymentRepository = deploymentRepository;
        deploymentRepository.addListener(deploymentListener);
    }

    public void destroy(){
        deploymentRepository.removeListener(deploymentListener);
    }

    @Override
    protected void processInvocation(final EJBReceiverInvocationContext receiverContext) throws Exception {
        final EJBClientInvocationContext invocation = receiverContext.getClientInvocationContext();
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
        final Map<AttachmentKey<?>, ?> privateAttachments = invocation.getAttachments();
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
            throw EjbLogger.ROOT_LOGGER.ejbNotFoundInDeployment(locator.getBeanName(), locator.getAppName(), locator.getModuleName(), locator.getDistinctName());
        }

        final ClonerConfiguration config = new ClonerConfiguration();
        config.setClassCloner(new LocalInvocationClassCloner(WildFlySecurityManager.getClassLoaderPrivileged(invocation.getInvokedProxy().getClass())));
        final ObjectCloner resultCloner = createCloner(config);
        if (async) {
            if (ejbComponent instanceof SessionBeanComponent) {
                final SessionBeanComponent component = (SessionBeanComponent) ejbComponent;
                final CancellationFlag flag = new CancellationFlag();
                final SecurityContext securityContext;
                if (WildFlySecurityManager.isChecking()) {
                    securityContext = AccessController.doPrivileged((PrivilegedAction<SecurityContext>) SecurityContextAssociation::getSecurityContext);
                } else {
                    securityContext = SecurityContextAssociation.getSecurityContext();
                }
                final StartupCountdown.Frame frame = StartupCountdown.current();
                final AsyncInvocationTask task = new AsyncInvocationTask(flag) {

                    @Override
                    protected Object runInvocation() throws Exception {
                        setSecurityContextOnAssociation(securityContext);
                        StartupCountdown.restore(frame);
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
                            StartupCountdown.restore(null);
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
    protected <T> StatefulEJBLocator<T> createSession(StatelessEJBLocator<T> statelessLocator) throws Exception {
        final EjbDeploymentInformation ejbInfo = findBean(statelessLocator.getAppName(), statelessLocator.getModuleName(), statelessLocator.getDistinctName(), statelessLocator.getBeanName());
        final EJBComponent component = ejbInfo.getEjbComponent();
        if (!(component instanceof StatefulSessionComponent)) {
            throw EjbLogger.ROOT_LOGGER.notStatefulSessionBean(statelessLocator.getAppName(), statelessLocator.getModuleName(), statelessLocator.getDistinctName(), statelessLocator.getBeanName());
        }
        final StatefulSessionComponent statefulComponent = (StatefulSessionComponent) component;
        final SessionID sessionID = statefulComponent.createSession();
        return new StatefulEJBLocator<T>(statelessLocator, sessionID, statefulComponent.getCache().getStrictAffinity());
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
                accessibleModules.put(module, createServiceURL(module));
            }
        }

        @Override
        public void deploymentAvailable(final DeploymentModuleIdentifier module, final ModuleDeployment moduleDeployment) {
            accessibleModules.put(module, createServiceURL(module));
        }

        private ServiceURL createServiceURL(final DeploymentModuleIdentifier moduleIdentifier) {
            final ServiceURL.Builder builder = new ServiceURL.Builder();
            builder.addAttribute("ejb-module", AttributeValue.fromString(moduleIdentifier.getApplicationName() + '/' + moduleIdentifier.getModuleName()));
            try {
                builder.setUri(new URI("local:localhost"));
            } catch (URISyntaxException ignored) {
                assert false : "Syntax error in programmatically created URI";
            }
            builder.setAbstractType("ejb");
            builder.setAbstractTypeAuthority("jboss");
            return builder.create();
        }

        @Override
        public void deploymentStarted(DeploymentModuleIdentifier module, ModuleDeployment moduleDeployment) {
        }

        @Override
        public void deploymentRemoved(final DeploymentModuleIdentifier module) {
            accessibleModules.remove(module);
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


    @Override
    public DiscoveryRequest discover(final ServiceType serviceType, final FilterSpec filterSpec, final DiscoveryResult result) {
        for (final ServiceURL url : accessibleModules.values()) {
            if (url.satisfies(filterSpec)) {
                final URI uri = url.getLocationURI();
                result.addMatch(uri);
            }
        }
        return DiscoveryRequest.NULL;
    }
}
