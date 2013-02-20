/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote.protocol.versionone;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.component.stateless.StatelessSessionComponent;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.invocation.InterceptorContext;
import org.jboss.marshalling.AbstractClassResolver;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.MessageOutputStream;
import org.xnio.IoUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * @author Jaikiran Pai
 */
public class MethodInvocationMessageHandler extends EJBIdentifierBasedMessageHandler {

    private static final char METHOD_PARAM_TYPE_SEPARATOR = ',';

    private static final byte HEADER_METHOD_INVOCATION_RESPONSE = 0x05;
    private static final byte HEADER_ASYNC_METHOD_NOTIFICATION = 0x0E;

    private final ExecutorService executorService;
    private final MarshallerFactory marshallerFactory;
    private final RemoteAsyncInvocationCancelStatusService remoteAsyncInvocationCancelStatus;

    public MethodInvocationMessageHandler(final DeploymentRepository deploymentRepository, final org.jboss.marshalling.MarshallerFactory marshallerFactory, final ExecutorService executorService,
                                   final RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatus) {
        super(deploymentRepository);
        this.marshallerFactory = marshallerFactory;
        this.executorService = executorService;
        this.remoteAsyncInvocationCancelStatus = asyncInvocationCancelStatus;
    }

    @Override
    public void processMessage(final ChannelAssociation channelAssociation, final MessageInputStream messageInputStream) throws IOException {

        final DataInputStream input = new DataInputStream(messageInputStream);
        // read the invocation id
        final short invocationId = input.readShort();

        // read the method name
        final String methodName = input.readUTF();
        // method signature
        String[] methodParamTypes = null;
        final String signature = input.readUTF();
        if (signature.isEmpty()) {
            methodParamTypes = new String[0];
        } else {
            methodParamTypes = signature.split(String.valueOf(METHOD_PARAM_TYPE_SEPARATOR));
        }

        // read the Locator
        // we use a mutable ClassResolver, so that we can switch to a different (and correct deployment CL)
        // midway through the unmarshalling of the stream
        final ClassLoaderSwitchingClassResolver classResolver = new ClassLoaderSwitchingClassResolver(Thread.currentThread().getContextClassLoader());
        final Unmarshaller unmarshaller = this.prepareForUnMarshalling(this.marshallerFactory, classResolver, input);
        // read the EJB info
        final String appName;
        final String moduleName;
        final String distinctName;
        final String beanName;
        try {
            appName = (String) unmarshaller.readObject();
            moduleName = (String) unmarshaller.readObject();
            distinctName = (String) unmarshaller.readObject();
            beanName = (String) unmarshaller.readObject();
        } catch (Throwable e) {
            throw EjbMessages.MESSAGES.failedToReadEjbInfo(e);
        }
        final EjbDeploymentInformation ejbDeploymentInformation = this.findEJB(appName, moduleName, distinctName, beanName);
        if (ejbDeploymentInformation == null) {
            this.writeNoSuchEJBFailureMessage(channelAssociation, invocationId, appName, moduleName, distinctName, beanName, null);
            return;
        }
        final ClassLoader tccl = SecurityActions.getContextClassLoader();
        Runnable runnable = null;
        try {
            //set the correct TCCL for unmarshalling
            SecurityActions.setContextClassLoader(ejbDeploymentInformation.getDeploymentClassLoader());
            // now switch the CL to the EJB deployment's CL so that the unmarshaller can use the
            // correct CL for the rest of the unmarshalling of the stream
            classResolver.switchClassLoader(ejbDeploymentInformation.getDeploymentClassLoader());
            // read the Locator
            final EJBLocator<?> locator;
            try {
                locator = (EJBLocator<?>) unmarshaller.readObject();
            } catch (Throwable e) {
                throw EjbMessages.MESSAGES.failedToReadEJBLocator(e);
            }
            final String viewClassName = locator.getViewType().getName();
            // Make sure it's a remote view
            if (!ejbDeploymentInformation.isRemoteView(viewClassName)) {
                this.writeNoSuchEJBFailureMessage(channelAssociation, invocationId, appName, moduleName, distinctName, beanName, viewClassName);
                return;
            }
            final ComponentView componentView = ejbDeploymentInformation.getView(viewClassName);
            final Method invokedMethod = this.findMethod(componentView, methodName, methodParamTypes);
            if (invokedMethod == null) {
                this.writeNoSuchEJBMethodFailureMessage(channelAssociation, invocationId, appName, moduleName, distinctName, beanName, viewClassName, methodName, methodParamTypes);
                return;
            }

            final Object[] methodParams = new Object[methodParamTypes.length];
            // un-marshall the method arguments
            if (methodParamTypes.length > 0) {
                for (int i = 0; i < methodParamTypes.length; i++) {
                    try {
                        methodParams[i] = unmarshaller.readObject();
                    } catch (Throwable e) {
                        // write out the failure
                        MethodInvocationMessageHandler.this.writeException(channelAssociation, MethodInvocationMessageHandler.this.marshallerFactory, invocationId, e, null);
                        return;
                    }
                }
            }
            // read the attachments
            final Map<String, Object> attachments;
            try {
                attachments = this.readAttachments(unmarshaller);
            } catch (Throwable e) {
                // write out the failure
                MethodInvocationMessageHandler.this.writeException(channelAssociation, MethodInvocationMessageHandler.this.marshallerFactory, invocationId, e, null);
                return;
            }
            // done with unmarshalling
            unmarshaller.finish();

            runnable = new Runnable() {

                @Override
                public void run() {
                    // check if it's async. If yes, then notify the client that's it's async method (so that
                    // it can unblock if necessary)
                    if (componentView.isAsynchronous(invokedMethod)) {
                        try {
                            MethodInvocationMessageHandler.this.writeAsyncMethodNotification(channelAssociation, invocationId);
                        } catch (Throwable t) {
                            // catch Throwable, so that we don't skip invoking the method, just because we
                            // failed to send a notification to the client that the method is an async method
                            EjbLogger.EJB3_LOGGER.failedToSendAsyncMethodIndicatorToClient(t, invokedMethod);
                        }
                    }

                    // invoke the method
                    Object result = null;
                    SecurityActions.remotingContextSetConnection(channelAssociation.getChannel().getConnection());
                    try {
                        result = invokeMethod(invocationId, componentView, invokedMethod, methodParams, locator, attachments);
                    } catch (Throwable throwable) {
                        try {
                            // write out the failure
                            MethodInvocationMessageHandler.this.writeException(channelAssociation, MethodInvocationMessageHandler.this.marshallerFactory, invocationId, throwable, attachments);
                        } catch (Throwable ioe) {
                            // we couldn't write out a method invocation failure message. So let's at least log the
                            // actual method invocation exception, for debugging/reference
                            EjbLogger.ROOT_LOGGER.errorInvokingMethod(throwable, invokedMethod, beanName, appName, moduleName, distinctName);
                            // now log why we couldn't send back the method invocation failure message
                            EjbLogger.ROOT_LOGGER.couldNotWriteMethodInvocation(ioe, invokedMethod, beanName, appName, moduleName, distinctName);
                            // close the channel unless this is a NotSerializableException
                            //as this does not represent a problem with the channel there is no
                            //need to close it (see AS7-3402)
                            if (!(ioe instanceof ObjectStreamException)) {
                                IoUtils.safeClose(channelAssociation.getChannel());
                            }
                        }
                        return;
                    } finally {
                        SecurityActions.remotingContextClear();
                    }
                    // write out the (successful) method invocation result to the channel output stream
                    try {
                        // attach any weak affinity if available
                        Affinity weakAffinity = null;
                        if (locator instanceof StatefulEJBLocator && componentView.getComponent() instanceof StatefulSessionComponent) {
                            final StatefulSessionComponent statefulSessionComponent = (StatefulSessionComponent) componentView.getComponent();
                            weakAffinity = MethodInvocationMessageHandler.this.getWeakAffinity(statefulSessionComponent, (StatefulEJBLocator<?>) locator);
                        } else if (componentView.getComponent() instanceof StatelessSessionComponent) {
                            final StatelessSessionComponent statelessSessionComponent = (StatelessSessionComponent) componentView.getComponent();
                            weakAffinity = statelessSessionComponent.getWeakAffinity();
                        }
                        if (weakAffinity != null) {
                            attachments.put(Affinity.WEAK_AFFINITY_CONTEXT_KEY, weakAffinity);
                        }
                        writeMethodInvocationResponse(channelAssociation, invocationId, result, attachments);
                    } catch (Throwable ioe) {
                        EjbLogger.ROOT_LOGGER.couldNotWriteMethodInvocation(ioe, invokedMethod, beanName, appName, moduleName, distinctName);
                        // close the channel unless this is a NotSerializableException
                        //as this does not represent a problem with the channel there is no
                        //need to close it (see AS7-3402)
                        if (!(ioe instanceof ObjectStreamException)) {
                            IoUtils.safeClose(channelAssociation.getChannel());
                        }
                        return;
                    }
                }
            };
        } finally {
            SecurityActions.setContextClassLoader(tccl);
        }
        // invoke the method and write out the response on a separate thread
        executorService.submit(runnable);
    }

    private Affinity getWeakAffinity(final StatefulSessionComponent statefulSessionComponent, final StatefulEJBLocator<?> statefulEJBLocator) {
        final SessionID sessionID = statefulEJBLocator.getSessionId();
        return statefulSessionComponent.getCache().getWeakAffinity(sessionID);
    }

    private Object invokeMethod(final short invocationId, final ComponentView componentView, final Method method, final Object[] args, final EJBLocator<?> ejbLocator, final Map<String, Object> attachments) throws Throwable {
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.setParameters(args);
        interceptorContext.setMethod(method);
        interceptorContext.putPrivateData(Component.class, componentView.getComponent());
        interceptorContext.putPrivateData(ComponentView.class, componentView);
        interceptorContext.putPrivateData(InvocationType.class, InvocationType.REMOTE);
        // setup the contextData on the (spec specified) InvocationContext
        final Map<String, Object> invocationContextData = new HashMap<String, Object>();
        interceptorContext.setContextData(invocationContextData);
        if (attachments != null) {
            // attach the attachments which were passed from the remote client
            for (final Map.Entry<String, Object> attachment : attachments.entrySet()) {
                if (attachment == null) {
                    continue;
                }
                final String key = attachment.getKey();
                final Object value = attachment.getValue();
                // these are private to JBoss EJB implementation and not meant to be visible to the
                // application, so add these attachments to the privateData of the InterceptorContext
                if (EJBClientInvocationContext.PRIVATE_ATTACHMENTS_KEY.equals(key)) {
                    final Map<Object, Object> privateAttachments = (Map<Object, Object>) value;
                    for (final Map.Entry<Object, Object> privateAttachment : privateAttachments.entrySet()) {
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
        if (ejbLocator instanceof StatefulEJBLocator) {
            interceptorContext.putPrivateData(SessionID.class, ((StatefulEJBLocator<?>) ejbLocator).getSessionId());
        } else if (ejbLocator instanceof EntityEJBLocator) {
            final Object primaryKey = ((EntityEJBLocator<?>) ejbLocator).getPrimaryKey();
            interceptorContext.putPrivateData(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, primaryKey);
        }
        if (componentView.isAsynchronous(method)) {
            final Component component = componentView.getComponent();
            if (!(component instanceof SessionBeanComponent)) {
                EjbLogger.EJB3_LOGGER.asyncMethodSupportedOnlyForSessionBeans(component.getComponentClass(), method);
                // just invoke normally
                return componentView.invoke(interceptorContext);
            }
            final CancellationFlag asyncInvocationCancellationFlag = new CancellationFlag();
            interceptorContext.putPrivateData(CancellationFlag.class, asyncInvocationCancellationFlag);
            // keep track of the cancellation flag for this invocation
            this.remoteAsyncInvocationCancelStatus.registerAsyncInvocation(invocationId, asyncInvocationCancellationFlag);
            try {
                return ((Future)componentView.invoke(interceptorContext)).get();
            } finally {
                // now that the async invocation is done, we no longer need to keep track of the
                // cancellation flag for this invocation
                this.remoteAsyncInvocationCancelStatus.asyncInvocationDone(invocationId);
            }
        } else {
            return componentView.invoke(interceptorContext);
        }
    }

    private Method findMethod(final ComponentView componentView, final String methodName, final String[] paramTypes) {
        final Set<Method> viewMethods = componentView.getViewMethods();
        for (final Method method : viewMethods) {
            if (method.getName().equals(methodName)) {
                final Class<?>[] methodParamTypes = method.getParameterTypes();
                if (methodParamTypes.length != paramTypes.length) {
                    continue;
                }
                boolean found = true;
                for (int i = 0; i < methodParamTypes.length; i++) {
                    if (!methodParamTypes[i].getName().equals(paramTypes[i])) {
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

    private void writeMethodInvocationResponse(final ChannelAssociation channelAssociation, final short invocationId, final Object result, final Map<String, Object> attachments) throws IOException {
        final DataOutputStream outputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Throwable e) {
            throw EjbMessages.MESSAGES.failedToOpenMessageOutputStream(e);
        }
        outputStream = new DataOutputStream(messageOutputStream);
        try {
            // write invocation response header
            outputStream.write(HEADER_METHOD_INVOCATION_RESPONSE);
            // write the invocation id
            outputStream.writeShort(invocationId);
            // write out the result
            final Marshaller marshaller = this.prepareForMarshalling(this.marshallerFactory, outputStream);
            marshaller.writeObject(result);
            // write the attachments
            this.writeAttachments(marshaller, attachments);
            // finish marshalling
            marshaller.finish();
        } finally {
            channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
            outputStream.close();
        }
    }


    private void writeAsyncMethodNotification(final ChannelAssociation channelAssociation, final short invocationId) throws IOException {
        final DataOutputStream outputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Throwable e) {
            throw EjbMessages.MESSAGES.failedToOpenMessageOutputStream(e);
        }
        outputStream = new DataOutputStream(messageOutputStream);
        try {
            // write the header
            outputStream.write(HEADER_ASYNC_METHOD_NOTIFICATION);
            // write the invocation id
            outputStream.writeShort(invocationId);
        } finally {
            channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
            outputStream.close();
        }
    }

    /**
     * A mutable {@link org.jboss.marshalling.ClassResolver}
     */
    private class ClassLoaderSwitchingClassResolver extends AbstractClassResolver {

        private ClassLoader currentClassLoader;

        ClassLoaderSwitchingClassResolver(final ClassLoader classLoader) {
            this.currentClassLoader = classLoader;
        }

        /**
         * Sets the passed <code>newCL</code> as the classloader which will be returned on
         * subsequent calls to {@link #getClassLoader()}
         *
         * @param newCL
         */
        void switchClassLoader(final ClassLoader newCL) {
            this.currentClassLoader = newCL;
        }

        @Override
        protected ClassLoader getClassLoader() {
            return this.currentClassLoader;
        }
    }
}
