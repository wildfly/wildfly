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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.interceptors.AsyncInvocationTask;
import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.remoting.RemotingAttachments;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;


/**
 * User: jpai
 */
class MethodInvocationMessageHandler extends EJBIdentifierBasedMessageHandler {

    private static final Logger logger = Logger.getLogger(MethodInvocationMessageHandler.class);

    private static final char METHOD_PARAM_TYPE_SEPARATOR = ',';

    private static final byte HEADER_METHOD_INVOCATION_RESPONSE = 0x05;
    private static final byte HEADER_ASYNC_METHOD_NOTIFICATION = 0x0E;

    private final ExecutorService executorService;

    MethodInvocationMessageHandler(final DeploymentRepository deploymentRepository, final String marshallingStrategy, final ExecutorService executorService) {
        super(deploymentRepository, marshallingStrategy);
        this.executorService = executorService;
    }

    @Override
    public void processMessage(final Channel channel, final MessageInputStream messageInputStream) throws IOException {

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
        // read the attachments
        final RemotingAttachments attachments = this.readAttachments(input);

        // read the Locator
        final UnMarshaller unMarshaller = MarshallerFactory.createUnMarshaller(this.marshallingStrategy);
        // we use a mutable ClassLoaderProvider, so that we can switch to a different (and correct deployment CL)
        // midway through the unmarshalling of the stream
        final ClassLoaderSwitchingClassLoaderProvider classLoaderProvider = new ClassLoaderSwitchingClassLoaderProvider(Thread.currentThread().getContextClassLoader());
        unMarshaller.start(input, classLoaderProvider);
        // read the EJB info
        final String appName;
        final String moduleName;
        final String distinctName;
        final String beanName;
        try {
            appName = (String) unMarshaller.readObject();
            moduleName = (String) unMarshaller.readObject();
            distinctName = (String) unMarshaller.readObject();
            beanName = (String) unMarshaller.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        final EjbDeploymentInformation ejbDeploymentInformation = this.findEJB(appName, moduleName, distinctName, beanName);
        if (ejbDeploymentInformation == null) {
            this.writeNoSuchEJBFailureMessage(channel, invocationId, appName, moduleName, distinctName, beanName, null);
            return;
        }
        final ClassLoader tccl = SecurityActions.getContextClassLoader();
        Runnable runnable = null;
        try {
            //set the correct TCCL for unmarshalling
            SecurityActions.setContextClassLoader(ejbDeploymentInformation.getDeploymentClassLoader());
            // now switch the CL to the EJB deployment's CL so that the unmarshaller can use the
            // correct CL for the rest of the unmarshalling of the stream
            classLoaderProvider.switchClassLoader(ejbDeploymentInformation.getDeploymentClassLoader());
            // read the Locator
            final EJBLocator locator;
            try {
                locator = (EJBLocator) unMarshaller.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            final String viewClassName = locator.getViewType().getName();
            if (!ejbDeploymentInformation.getViewNames().contains(viewClassName)) {
                this.writeNoSuchEJBFailureMessage(channel, invocationId, appName, moduleName, distinctName, beanName, viewClassName);
                return;
            }
            // TODO: Add a check for remote view
            final ComponentView componentView = ejbDeploymentInformation.getView(viewClassName);
            final Method invokedMethod = this.findMethod(componentView, methodName, methodParamTypes);
            if (invokedMethod == null) {
                this.writeNoSuchEJBMethodFailureMessage(channel, invocationId, appName, moduleName, distinctName, beanName, viewClassName, methodName, methodParamTypes);
                return;
            }

            final Object[] methodParams = new Object[methodParamTypes.length];
            // un-marshall the method arguments
            if (methodParamTypes.length > 0) {
                for (int i = 0; i < methodParamTypes.length; i++) {
                    try {
                        methodParams[i] = unMarshaller.readObject();
                    } catch (ClassNotFoundException cnfe) {
                        // TODO: Write out invocation failure to channel outstream
                        throw new RuntimeException(cnfe);
                    }
                }
            }
            unMarshaller.finish();

            runnable = new Runnable() {

                @Override
                public void run() {
                    // check if it's async. If yes, then notify the client that's it's async method (so that
                    // it can unblock if necessary)
                    if (componentView.isAsynchronous(invokedMethod)) {
                        try {
                            MethodInvocationMessageHandler.this.writeAsyncMethodNotification(channel, invocationId);
                        } catch (Throwable t) {
                            // catch Throwable, so that we don't skip invoking the method, just because we
                            // failed to send a notification to the client that the method is an async method
                            logger.warn("Method " + invokedMethod + " was a async method but the client could not be informed about the same. This will mean that the client might block till the method completes", t);
                        }
                    }

                    // invoke the method
                    Object result = null;
                    try {
                        result = invokeMethod(componentView, invokedMethod, methodParams, locator, attachments);
                    } catch (Throwable throwable) {
                        try {
                            // write out the failure
                            MethodInvocationMessageHandler.this.writeException(channel, invocationId, throwable, attachments);
                        } catch (IOException ioe) {
                            // we couldn't write out a method invocation failure message. So let's atleast log the
                            // actual method invocation exception, for debugging/reference
                            logger.error("Error invoking method " + invokedMethod + " on bean named " + beanName
                                    + " for appname " + appName + " modulename " + moduleName + " distinctname " + distinctName, throwable);
                            // now log why we couldn't send back the method invocation failure message
                            logger.error("Could not write method invocation failure for method " + invokedMethod + " on bean named " + beanName
                                    + " for appname " + appName + " modulename " + moduleName + " distinctname " + distinctName + " due to ", ioe);
                            // close the channel
                            IoUtils.safeClose(channel);
                            return;
                        }
                    }
                    // write out the (successful) method invocation result to the channel output stream
                    try {
                        writeMethodInvocationResponse(channel, invocationId, result, attachments);
                    } catch (IOException ioe) {
                        logger.error("Could not write method invocation result for method " + invokedMethod + " on bean named " + beanName
                                + " for appname " + appName + " modulename " + moduleName + " distinctname " + distinctName + " due to ", ioe);
                        // close the channel
                        IoUtils.safeClose(channel);
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

    private Object invokeMethod(final ComponentView componentView, final Method method, final Object[] args, final EJBLocator ejbLocator, final RemotingAttachments attachments) throws Throwable {
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.setParameters(args);
        interceptorContext.setMethod(method);
        interceptorContext.setContextData(new HashMap<String, Object>());
        interceptorContext.putPrivateData(Component.class, componentView.getComponent());
        interceptorContext.putPrivateData(ComponentView.class, componentView);
        if (attachments != null) {
            // attach the RemotingAttachments
            interceptorContext.putPrivateData(RemotingAttachments.class, attachments);
        }
        // add the session id to the interceptor context, if it's a stateful ejb locator
        if (ejbLocator instanceof StatefulEJBLocator) {
            interceptorContext.putPrivateData(SessionID.SESSION_ID_KEY, ((StatefulEJBLocator) ejbLocator).getSessionId());
        } else if (ejbLocator instanceof EntityEJBLocator) {
            final Object primaryKey = ((EntityEJBLocator) ejbLocator).getPrimaryKey();
            interceptorContext.putPrivateData(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, primaryKey);
        }
        if (componentView.isAsynchronous(method)) {
            final Component component = componentView.getComponent();
            if (!(component instanceof SessionBeanComponent)) {
                logger.warn("Asynchronous invocations are only supported on session beans. Bean class " + component.getComponentClass()
                        + " is not a session bean, invocation on method " + method + " will have no asynchronous semantics");
                // just invoke normally
                return componentView.invoke(interceptorContext);
            }
            // it's really a async method invocation on a session bean. So treat it accordingly
            final SessionBeanComponent sessionBeanComponent = (SessionBeanComponent) componentView.getComponent();
            final CancellationFlag cancellationFlag = new CancellationFlag();
            // add the cancellation flag to the interceptor context
            interceptorContext.putPrivateData(CancellationFlag.class, cancellationFlag);

            final AsyncInvocationTask asyncInvocationTask = new AsyncInvocationTask(cancellationFlag) {
                @Override
                protected Object runInvocation() throws Exception {
                    return componentView.invoke(interceptorContext);
                }
            };
            // invoke
            sessionBeanComponent.getAsynchronousExecutor().submit(asyncInvocationTask);
            // wait/block for the bean invocation to complete and get the real result to be returned to the client
            return asyncInvocationTask.get();
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

    private void writeMethodInvocationResponse(final Channel channel, final short invocationId, final Object result, final RemotingAttachments attachments) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(channel.writeMessage());
        try {
            // write invocation response header
            outputStream.write(HEADER_METHOD_INVOCATION_RESPONSE);
            // write the invocation id
            outputStream.writeShort(invocationId);
            // write the attachments
            this.writeAttachments(outputStream, attachments);

            // write out the result
            final Marshaller marshaller = MarshallerFactory.createMarshaller(this.marshallingStrategy);
            marshaller.start(outputStream);
            marshaller.writeObject(result);
            marshaller.finish();
        } finally {
            outputStream.close();
        }
    }


    private void writeAsyncMethodNotification(final Channel channel, final short invocationId) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(channel.writeMessage());
        try {
            // write the header
            outputStream.write(HEADER_ASYNC_METHOD_NOTIFICATION);
            // write the invocation id
            outputStream.writeShort(invocationId);
        } finally {
            outputStream.close();
        }
    }

    /**
     * A mutable {@link org.jboss.as.ejb3.remote.protocol.versionone.UnMarshaller.ClassLoaderProvider}
     */
    private class ClassLoaderSwitchingClassLoaderProvider implements UnMarshaller.ClassLoaderProvider {

        private ClassLoader currentClassLoader;

        ClassLoaderSwitchingClassLoaderProvider(final ClassLoader classLoader) {
            this.currentClassLoader = classLoader;
        }

        @Override
        public ClassLoader provideClassLoader() {
            return this.currentClassLoader;
        }

        /**
         * Sets the passed <code>newCL</code> as the classloader which will be returned on
         * subsequent calls to {@link #provideClassLoader()}
         *
         * @param newCL
         */
        void switchClassLoader(final ClassLoader newCL) {
            this.currentClassLoader = newCL;
        }
    }
}
