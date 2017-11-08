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
import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.AttachmentKey;
import org.jboss.ejb.client.AttachmentKeys;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.ejb.client.EJBReceiverInvocationContext;
import org.jboss.ejb.client.EJBReceiverSessionCreationContext;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.TransactionID;
import org.jboss.invocation.InterceptorContext;
import org.jboss.marshalling.cloner.ClassLoaderClassCloner;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectCloners;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * {@link EJBReceiver} for local same-VM invocations. This handles all invocations on remote interfaces
 * within the server JVM.
 *
 * @author Stuart Douglas
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class LocalEjbReceiver extends EJBReceiver {

    private static final Object[] EMPTY_OBJECT_ARRAY = {};
    private static final EJBReceiverInvocationContext.ResultProducer.Immediate NULL_RESULT = new EJBReceiverInvocationContext.ResultProducer.Immediate(null);
    private static final AttachmentKey<CancellationFlag> CANCELLATION_FLAG_ATTACHMENT_KEY = new AttachmentKey<>();

    private final DeploymentRepository deploymentRepository;

    private final boolean allowPassByReference;

    public LocalEjbReceiver(final boolean allowPassByReference, final DeploymentRepository deploymentRepository) {
        this.allowPassByReference = allowPassByReference;
        this.deploymentRepository = deploymentRepository;
    }

    @Override
    protected void processInvocation(final EJBReceiverInvocationContext receiverContext) {
        final EJBClientInvocationContext invocation = receiverContext.getClientInvocationContext();
        final EJBLocator<?> locator = invocation.getLocator();
        final EjbDeploymentInformation ejb = findBean(locator);
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

        final boolean async = view.isAsynchronous(method) || invocation.isClientAsync();

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
        interceptorContext.setTransaction(invocation.getTransaction());
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
            if (invocationContextData != null) for (Map.Entry<String, Object> entry : invocationContextData.entrySet()) {
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

        if (locator.isStateful()) {
            interceptorContext.putPrivateData(SessionID.class, locator.asStateful().getSessionId());
        } else if (locator instanceof EntityEJBLocator) {
            throw EjbLogger.ROOT_LOGGER.ejbNotFoundInDeployment(locator);
        }

        final ClonerConfiguration config = new ClonerConfiguration();
        config.setClassCloner(new LocalInvocationClassCloner(WildFlySecurityManager.getClassLoaderPrivileged(invocation.getInvokedProxy().getClass())));
        final ObjectCloner resultCloner = createCloner(config);
        if (async) {
            if (ejbComponent instanceof SessionBeanComponent) {
                final CancellationFlag flag = new CancellationFlag();
                final SessionBeanComponent component = (SessionBeanComponent) ejbComponent;
                final boolean isAsync = view.isAsynchronous(method);
                final boolean oneWay = isAsync && method.getReturnType() == void.class;
                final boolean isSessionBean = view.getComponent() instanceof SessionBeanComponent;
                if (isAsync && isSessionBean) {
                    if (! oneWay) {
                        interceptorContext.putPrivateData(CancellationFlag.class, flag);
                    }
                }
                final SecurityContext securityContext;
                final SecurityDomain securityDomain;
                if (WildFlySecurityManager.isChecking()) {
                    securityContext = AccessController.doPrivileged((PrivilegedAction<SecurityContext>) SecurityContextAssociation::getSecurityContext);
                    securityDomain = AccessController.doPrivileged((PrivilegedAction<SecurityDomain>) SecurityDomain::getCurrent);
                } else {
                    securityContext = SecurityContextAssociation.getSecurityContext();
                    securityDomain = SecurityDomain.getCurrent();
                }
                final SecurityIdentity securityIdentity = securityDomain != null ? securityDomain.getCurrentSecurityIdentity() : null;
                final StartupCountdown.Frame frame = StartupCountdown.current();
                final Runnable task = () -> {
                    if (! flag.runIfNotCancelled()) {
                        receiverContext.requestCancelled();
                        return;
                    }
                    setSecurityContextOnAssociation(securityContext);
                    StartupCountdown.restore(frame);
                    try {
                        final Object result;
                        try {
                            result = view.invoke(interceptorContext);
                        } catch (Exception e) {
                            // WFLY-4331 - clone the exception of an async task
                            receiverContext.resultReady(new CloningExceptionProducer(resultCloner, e));
                            return;
                        }
                        // if the result is null, there is no cloning needed
                        if(result == null) {
                            receiverContext.resultReady(NULL_RESULT);
                            return;
                        }
                        // WFLY-4331 - clone the result of an async task
                        if(result instanceof Future) {
                            // blocking is very unlikely here, so just defer interrupts when they happen
                            boolean intr = Thread.interrupted();
                            Object asyncValue;
                            try {
                                for (;;) try {
                                    asyncValue = ((Future<?>)result).get();
                                    break;
                                } catch (InterruptedException e) {
                                    intr = true;
                                } catch (ExecutionException e) {
                                    // WFLY-4331 - clone the exception of an async task
                                    receiverContext.resultReady(new CloningExceptionProducer(resultCloner, e));
                                    return;
                                }
                            } finally {
                                if (intr) Thread.currentThread().interrupt();
                            }
                            // if the return value is null, there is no cloning needed
                            if (asyncValue == null) {
                                receiverContext.resultReady(NULL_RESULT);
                                return;
                            }
                            receiverContext.resultReady(new CloningResultProducer(invocation, resultCloner, asyncValue, allowPassByReference));
                            return;
                        }
                        receiverContext.resultReady(new CloningResultProducer(invocation, resultCloner, result, allowPassByReference));
                    } finally {
                        StartupCountdown.restore(null);
                        clearSecurityContextOnAssociation();
                    }
                };
                invocation.putAttachment(CANCELLATION_FLAG_ATTACHMENT_KEY, flag);
                interceptorContext.putPrivateData(CancellationFlag.class, flag);
                final ExecutorService executor = component.getAsynchronousExecutor();
                if (executor == null) {
                    receiverContext.resultReady(new EJBReceiverInvocationContext.ResultProducer.Failed(
                        EjbLogger.ROOT_LOGGER.executorIsNull()
                    ));
                } else {
                    // this normally isn't necessary unless the client didn't detect that it was an async method for some reason
                    receiverContext.proceedAsynchronously();
                    executor.execute(securityIdentity == null ? task : () -> securityIdentity.runAs(task));
                }
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
                receiverContext.resultReady(new CloningExceptionProducer(resultCloner, e));
                return;
            }
            receiverContext.resultReady(new CloningResultProducer(invocation, resultCloner, result, allowPassByReference));

            for(Map.Entry<String, Object> entry : interceptorContext.getContextData().entrySet()) {
                if (entry.getValue() instanceof Serializable) {
                    invocation.getContextData().put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    protected boolean cancelInvocation(final EJBReceiverInvocationContext receiverContext, final boolean cancelIfRunning) {
        CancellationFlag flag = receiverContext.getClientInvocationContext().getAttachment(CANCELLATION_FLAG_ATTACHMENT_KEY);
        return flag != null && flag.cancel(cancelIfRunning);
    }

    static final class CloningResultProducer implements EJBReceiverInvocationContext.ResultProducer {
        private final EJBClientInvocationContext invocation;
        private final ObjectCloner resultCloner;
        private final Object result;
        private final boolean allowPassByReference;

        CloningResultProducer(final EJBClientInvocationContext invocation, final ObjectCloner resultCloner, final Object result, final boolean allowPassByReference) {
            this.invocation = invocation;
            this.resultCloner = resultCloner;
            this.result = result;
            this.allowPassByReference = allowPassByReference;
        }

        public Object getResult() throws Exception {
            return LocalEjbReceiver.clone(invocation.getInvokedMethod().getReturnType(), resultCloner, result, allowPassByReference);
        }

        public void discardResult() {
        }
    }

    static final class CloningExceptionProducer implements EJBReceiverInvocationContext.ResultProducer {
        private final ObjectCloner resultCloner;
        private final Exception exception;

        CloningExceptionProducer(final ObjectCloner resultCloner, final Exception exception) {
            this.resultCloner = resultCloner;
            this.exception = exception;
        }

        public Object getResult() throws Exception {
            throw (Exception) LocalEjbReceiver.clone(resultCloner, exception);
        }

        public void discardResult() {
        }
    }



    private ObjectCloner createCloner(final ClonerConfiguration paramConfig) {
        ObjectCloner parameterCloner;
        if(WildFlySecurityManager.isChecking()) {
            parameterCloner = WildFlySecurityManager.doUnchecked((PrivilegedAction<ObjectCloner>) () -> ObjectCloners.getSerializingObjectClonerFactory().createCloner(paramConfig));
        } else {
            parameterCloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(paramConfig);
        }
        return parameterCloner;
    }

    protected SessionID createSession(final EJBReceiverSessionCreationContext receiverContext) throws Exception {
        final StatelessEJBLocator<?> statelessLocator = receiverContext.getClientInvocationContext().getLocator().asStateless();
        final EjbDeploymentInformation ejbInfo = findBean(statelessLocator);
        final EJBComponent component = ejbInfo.getEjbComponent();
        if (!(component instanceof StatefulSessionComponent)) {
            throw EjbLogger.ROOT_LOGGER.notStatefulSessionBean(statelessLocator.getAppName(), statelessLocator.getModuleName(), statelessLocator.getDistinctName(), statelessLocator.getBeanName());
        }
        component.waitForComponentStart();
        return ((StatefulSessionComponent) component).createSession();
    }

    static Object clone(final Class<?> target, final ObjectCloner cloner, final Object object, final boolean allowPassByReference) {
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

        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                try {
                    return cloner.clone(object);
                } catch (Exception e) {
                    throw EjbLogger.ROOT_LOGGER.failedToMarshalEjbParameters(e);
                }
            });
        } else {
            try {
                return cloner.clone(object);
            } catch (Exception e) {
                throw EjbLogger.ROOT_LOGGER.failedToMarshalEjbParameters(e);
            }
        }
    }


    private EjbDeploymentInformation findBean(final EJBLocator<?> locator) {
        final String appName = locator.getAppName();
        final String moduleName = locator.getModuleName();
        final String distinctName = locator.getDistinctName();
        final String beanName = locator.getBeanName();
        final DeploymentModuleIdentifier moduleIdentifier = new DeploymentModuleIdentifier(appName, moduleName, distinctName);
        final ModuleDeployment module = deploymentRepository.getValue().getModules().get(moduleIdentifier);
        if (module == null) {
            throw EjbLogger.ROOT_LOGGER.unknownDeployment(locator);
        }
        final EjbDeploymentInformation ejbInfo = module.getEjbs().get(beanName);
        if (ejbInfo == null) {
            throw EjbLogger.ROOT_LOGGER.ejbNotFoundInDeployment(locator);
        }
        return ejbInfo;
    }

    private static void setSecurityContextOnAssociation(final SecurityContext sc) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            SecurityContextAssociation.setSecurityContext(sc);
            return null;
        });
    }

    private static void clearSecurityContextOnAssociation() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            SecurityContextAssociation.clearSecurityContext();
            return null;
        });
    }
}
