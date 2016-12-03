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
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EJBMethodLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.TransactionID;
import org.jboss.ejb.protocol.remote.RemoteServer;
import org.jboss.invocation.AsynchronousInterceptor;
import org.jboss.invocation.InterceptorContext;

import javax.ejb.EJBException;
import javax.transaction.Transaction;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class EJBRemoteServerAssociation implements RemoteServer.Association{

    private final DeploymentRepository deploymentRepository;
    private final RemoteAsyncInvocationCancelStatusService remoteAsyncInvocationCancelStatus;
    private final ExecutorService executorService;

    public EJBRemoteServerAssociation(final DeploymentRepository deploymentRepository, final RemoteAsyncInvocationCancelStatusService remoteAsyncInvocationCancelStatus, final ExecutorService executorService) {
        this.deploymentRepository = deploymentRepository;
        this.remoteAsyncInvocationCancelStatus = remoteAsyncInvocationCancelStatus;
        this.executorService = executorService;
    }

    @Override
    public ClassLoader mapClassLoader(String appName, String moduleName) {
        return findEJB(appName,moduleName).getDeploymentClassLoader();
    }

    @Override
    public AsynchronousInterceptor.CancellationHandle receiveInvocationRequest(RemoteServer.IncomingInvocation incomingInvocation) {

        final int invocationId = incomingInvocation.getInvId();

        final EJBLocator<?> ejbLocator = incomingInvocation.getEjbLocator();

        final String appName = ejbLocator.getAppName();
        final String moduleName = ejbLocator.getModuleName();
        final String distinctName = ejbLocator.getDistinctName();
        final String beanName = ejbLocator.getBeanName();

        final Map<String, Object> attachments = incomingInvocation.getAttachments();

        final EjbDeploymentInformation ejbDeploymentInformation = findEJB(appName, moduleName, distinctName, beanName);


        Runnable runnable = new Runnable() {

            @Override
            public void run() {

                final String viewClassName = incomingInvocation.getEjbLocator().getViewType().getName();
                if (!ejbDeploymentInformation.isRemoteView(viewClassName)) {
                    incomingInvocation.writeNoSuchEJBFailureMessage();
                    return;
                }
                final ComponentView componentView = ejbDeploymentInformation.getView(viewClassName);

                final Method invokedMethod = findMethod(componentView,  incomingInvocation.getMethodLocator());
                if (invokedMethod == null) {
                    incomingInvocation.writeNoSuchEJBMethodFailureMessage();
                    return;
                }

                // check if it's async. If yes, then notify the client that's it's async method (so that
                // it can unblock if necessary)
                if (componentView.isAsynchronous(invokedMethod)) {
                    try {
                        incomingInvocation.writeAsyncMethodNotification();
                    } catch (Throwable t) {
                        // catch Throwable, so that we don't skip invoking the method, just because we
                        // failed to send a notification to the client that the method is an async method
                        EjbLogger.REMOTE_LOGGER.failedToSendAsyncMethodIndicatorToClient(t, invokedMethod);
                    }
                }

                // invoke the method
                Object result = null;
                //SecurityActions.remotingContextSetConnection(channelAssociation.getChannel().getConnection());
                try {
                    result = invokeMethod(invocationId, componentView, invokedMethod, incomingInvocation);
                } catch (Throwable throwable) {
                    try {
                        // if the EJB is shutting down when the invocation was done, then it's as good as the EJB not being available. The client has to know about this as
                        // a "no such EJB" failure so that it can retry the invocation on a different node if possible.
                        if (throwable instanceof EJBComponentUnavailableException) {
                            EjbLogger.EJB3_INVOCATION_LOGGER.debugf("Cannot handle method invocation: %s on bean: %s due to EJB component unavailability exception. Returning a no such EJB available message back to client", invokedMethod, beanName);
                             incomingInvocation.writeNoSuchEJBFailureMessage();
                            // WFLY-7139
                        } else if (throwable instanceof ComponentIsStoppedException) {
                            //TODO Elytron
                            EjbLogger.EJB3_INVOCATION_LOGGER.debugf("Cannot handle method invocation: %s on bean: %s due to EJB component stopped exception. Returning a no such EJB available message back to client", invokedMethod, beanName);
                            incomingInvocation.writeNoSuchEJBFailureMessage();
                        } else {
                            // write out the failure
                            Throwable throwableToWrite = throwable;
                            final Throwable cause = throwable.getCause();
                            if (componentView.getComponent() instanceof StatefulSessionComponent && throwable instanceof EJBException && cause != null) {
                                if (!(componentView.getComponent().isRemotable(cause))) {
                                    // Avoid serializing the cause of the exception in case it is not remotable
                                    // Client might not be able to deserialize and throw ClassNotFoundException
                                    throwableToWrite = new EJBException(throwable.getLocalizedMessage());
                                }
                            }
                            incomingInvocation.writeThrowable(throwableToWrite);
                        }
                    } catch (Throwable ioe) {

                    }
                }
                try {
                    // attach any weak affinity if available
                    Affinity weakAffinity = null;
                    if (ejbLocator instanceof StatefulEJBLocator && componentView.getComponent() instanceof StatefulSessionComponent) {
                        final StatefulSessionComponent statefulSessionComponent = (StatefulSessionComponent) componentView.getComponent();
                        weakAffinity = getWeakAffinity(statefulSessionComponent, (StatefulEJBLocator<?>) ejbLocator);
                    } else if (componentView.getComponent() instanceof StatelessSessionComponent) {
                        final StatelessSessionComponent statelessSessionComponent = (StatelessSessionComponent) componentView.getComponent();
                        weakAffinity = statelessSessionComponent.getWeakAffinity();
                    }
                    if (weakAffinity != null && !weakAffinity.equals(Affinity.NONE)) {
                        attachments.put(Affinity.WEAK_AFFINITY_CONTEXT_KEY, weakAffinity);
                    }
                    incomingInvocation.writeResponse(result);
                } catch (Throwable ioe) {
                    boolean isAsyncVoid = componentView.isAsynchronous(invokedMethod) && invokedMethod.getReturnType().equals(Void.TYPE);
                    if (!isAsyncVoid)
                        EjbLogger.REMOTE_LOGGER.couldNotWriteMethodInvocation(ioe, invokedMethod, beanName, appName, moduleName, distinctName);
                    return;
                }
            }
        };
        // invoke the method and write out the response on a separate thread
        if(executorService != null) {
            executorService.submit( runnable );
        } else {
            runnable.run();
        }
        //TODO Elytron - proper callback
        return new AsynchronousInterceptor.CancellationHandle() {
            @Override
            public void cancel(boolean b) {

            }
        };
    }

    @Override
    public void receiveSessionOpenRequest(final RemoteServer.IncomingSessionOpen incomingSessionOpen) {

        final String appName = incomingSessionOpen.getIdentifier().getAppName();
        final String moduleName = incomingSessionOpen.getIdentifier().getModuleName();
        final String beanName = incomingSessionOpen.getIdentifier().getBeanName();
        final String distinctName = incomingSessionOpen.getIdentifier().getDistinctName();

        final EjbDeploymentInformation ejbDeploymentInformation = findEJB(appName, moduleName, distinctName, beanName);
        if (ejbDeploymentInformation == null) {
            incomingSessionOpen.writeNoSuchEJBFailureMessage();
            return;
        }
        final Component component = ejbDeploymentInformation.getEjbComponent();
        if (!(component instanceof StatefulSessionComponent)) {
            incomingSessionOpen.writeNotStatefulSessionBean();
            return;
        }
        final StatefulSessionComponent statefulSessionComponent = (StatefulSessionComponent) component;
        // generate the session id and write out the response on a separate thread
        SessionIDGeneratorTask task = new SessionIDGeneratorTask(statefulSessionComponent, incomingSessionOpen);
        if(executorService != null) {
            executorService.submit(task);
        } else {
            task.run();
        }
    }

    @Override
    public void closed() {

    }

    @Override
    public Transaction lookupLegacyTransaction(TransactionID transactionId) {
        return null;
    }

    protected EjbDeploymentInformation findEJB(final String appName, final String moduleName, final String distinctName, final String beanName) {
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

    //TODO Elytron - find proper way match appName/moduleName to classloader
    protected EjbDeploymentInformation findEJB(final String appName, final String moduleName) {
        final DeploymentModuleIdentifier ejbModule = new DeploymentModuleIdentifier(appName, moduleName, "");
        final Map<DeploymentModuleIdentifier, ModuleDeployment> modules = this.deploymentRepository.getStartedModules();
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        final ModuleDeployment moduleDeployment = modules.get(ejbModule);
        if (moduleDeployment == null) {
            return null;
        }
        return moduleDeployment.getEjbs().entrySet().iterator().next().getValue();
    }



    private Object invokeMethod(final int invocationId, final ComponentView componentView, final Method method, final RemoteServer.IncomingInvocation incomingInvocation) throws Throwable {
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.setParameters(incomingInvocation.getParameters());
        interceptorContext.setMethod(method);
        interceptorContext.putPrivateData(Component.class, componentView.getComponent());
        interceptorContext.putPrivateData(ComponentView.class, componentView);
        interceptorContext.putPrivateData(InvocationType.class, InvocationType.REMOTE);
        // setup the contextData on the (spec specified) InvocationContext
        final Map<String, Object> invocationContextData = new HashMap<String, Object>();
        interceptorContext.setContextData(invocationContextData);
        if (incomingInvocation.getAttachments() != null) {
            // attach the attachments which were passed from the remote client
            for (final Map.Entry<String, Object> attachment : incomingInvocation.getAttachments().entrySet()){
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
        if (incomingInvocation.getEjbLocator() instanceof StatefulEJBLocator) {
            interceptorContext.putPrivateData(SessionID.class, ((StatefulEJBLocator<?>) incomingInvocation.getEjbLocator()).getSessionId());
        }
        if (componentView.isAsynchronous(method)) {
            final Component component = componentView.getComponent();
            if (!(component instanceof SessionBeanComponent)) {
                EjbLogger.REMOTE_LOGGER.asyncMethodSupportedOnlyForSessionBeans(component.getComponentClass(), method);
                // just invoke normally
                return componentView.invoke(interceptorContext);
            }
            final CancellationFlag asyncInvocationCancellationFlag = new CancellationFlag();
            interceptorContext.putPrivateData(CancellationFlag.class, asyncInvocationCancellationFlag);
            // keep track of the cancellation flag for this invocation
            this.remoteAsyncInvocationCancelStatus.registerAsyncInvocation(invocationId, asyncInvocationCancellationFlag);
            try {
                final Object result = componentView.invoke(interceptorContext);
                return result == null ? null : ((Future<?>) result).get();
            } finally {
                // now that the async invocation is done, we no longer need to keep track of the
                // cancellation flag for this invocation
                this.remoteAsyncInvocationCancelStatus.asyncInvocationDone(invocationId);
            }
        } else {
            return componentView.invoke(interceptorContext);
        }
    }

    private Method findMethod(final ComponentView componentView, final EJBMethodLocator ejbMethodLocator) {
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

    private Affinity getWeakAffinity(final StatefulSessionComponent statefulSessionComponent, final StatefulEJBLocator<?> statefulEJBLocator) {
        final SessionID sessionID = statefulEJBLocator.getSessionId();
        return statefulSessionComponent.getCache().getWeakAffinity(sessionID);
    }

    /**
     * Task for generation a session id when a session open request is received
     */
    private class SessionIDGeneratorTask implements Runnable {

        private final StatefulSessionComponent statefulSessionComponent;
        private final RemoteServer.IncomingSessionOpen incomingSessionOpen;

        SessionIDGeneratorTask(final StatefulSessionComponent statefulSessionComponent, final RemoteServer.IncomingSessionOpen incomingSessionOpen) {
            this.statefulSessionComponent = statefulSessionComponent;
            this.incomingSessionOpen = incomingSessionOpen;
        }

        @Override
        public void run() {
            final SessionID sessionID;
            try {
                sessionID = statefulSessionComponent.createSessionRemote();
            } catch (Exception t) {
                EjbLogger.REMOTE_LOGGER.exceptionGeneratingSessionId(t, statefulSessionComponent.getComponentName(), incomingSessionOpen.getInvId());
                incomingSessionOpen.writeThrowable(t);
                return;
            }
            incomingSessionOpen.writeResponse(sessionID);
        }
    }

}