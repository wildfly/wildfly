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
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.ejb.client.remoting.RemotingAttachments;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;


/**
 * User: jpai
 */
class MethodInvocationMessageHandler extends AbstractMessageHandler {

    private static final Logger logger = Logger.getLogger(MethodInvocationMessageHandler.class);

    private static final char METHOD_PARAM_TYPE_SEPARATOR = ',';

    private static final byte HEADER_MESSAGE_INVOCATION_RESPONSE = 0x05;

    MethodInvocationMessageHandler(final DeploymentRepository deploymentRepository, final String marshallingStrategy) {
        super(deploymentRepository, marshallingStrategy);
    }

    @Override
    public void processMessage(final Channel channel, final MessageInputStream messageInputStream) throws IOException {
        final DataInputStream input = new DataInputStream(messageInputStream);
        // read the invocation id
        final short invocationId = input.readShort();
        // read the ejb module info
        String appName = input.readUTF();
        if (appName.isEmpty()) {
            appName = null;
        }
        final String moduleName = input.readUTF();
        final String distinctName = input.readUTF();
        final String beanName = input.readUTF();
        final String viewClassName = input.readUTF();

        final EjbDeploymentInformation ejbDeploymentInformation = this.findEJB(appName, moduleName, distinctName, beanName);
        if (ejbDeploymentInformation == null) {
            this.writeNoSuchEJBFailureMessage(channel, invocationId, appName, moduleName, distinctName, beanName, viewClassName);
            return;
        }
        final ComponentView componentView = ejbDeploymentInformation.getView(viewClassName);
        if (componentView == null) {
            this.writeNoSuchEJBFailureMessage(channel, invocationId, appName, moduleName, distinctName, beanName, viewClassName);
            return;
        }
        // TODO: Add a check for remote view

        final String methodName = input.readUTF();
        // method signature
        String[] methodParamTypes = null;
        final String signature = input.readUTF();
        if (signature.isEmpty()) {
            methodParamTypes = new String[0];
        } else {
            methodParamTypes = signature.split(String.valueOf(METHOD_PARAM_TYPE_SEPARATOR));
        }
        final Method invokedMethod = this.findMethod(componentView, methodName, methodParamTypes);
        if (invokedMethod == null) {
            // TODO: Write out NoSuchMethod invocation failure to channel outstream
            return;
        }
        // read the attachments
        final RemotingAttachments attachments = this.readAttachments(input);

        final Object[] methodParams = new Object[methodParamTypes.length];
        // un-marshall the method arguments
        // TODO: The protocol should actually write out the number of object params being passed
        // because even if the signature might have a param type the method could be invoked without
        // params (for example: someMethod(String... optionalParams)
        if (methodParamTypes.length > 0) {
            final UnMarshaller unMarshaller = MarshallerFactory.createUnMarshaller(this.marshallingStrategy);
            final ClassLoader beanClassLoader = ejbDeploymentInformation.getDeploymentClassLoader();

            unMarshaller.start(input, beanClassLoader);
            for (int i = 0; i < methodParamTypes.length; i++) {
                try {
                    methodParams[i] = unMarshaller.readObject();
                } catch (ClassNotFoundException cnfe) {
                    // TODO: Write out invocation failure to channel outstream
                    //throw new RuntimeException(cnfe);
                    return;
                }
            }
            unMarshaller.finish();
        }

        // now invoke the target method
        // TODO: The method invocation (and the subsequent writing of result to the channel stream) should happen
        // on a different thread
        Object result = null;
        try {
            result = this.invokeMethod(componentView, invokedMethod, methodParams, attachments);
        } catch (Throwable throwable) {
            logger.error("Error invoking method " + invokedMethod + " on bean named " + beanName + " in app: " + appName + " module: " + moduleName +
                    " distinctname: " + distinctName, throwable);
            // TODO: write out the exception as a failure to the channel output stream
            return;
        }
        // write out the result to the channel output stream
        this.writeMethodInvocationResponse(channel, invocationId, result, attachments);
    }

    private Object invokeMethod(final ComponentView componentView, final Method method, final Object[] args, final RemotingAttachments attachments) throws Throwable {
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.setParameters(args);
        interceptorContext.setMethod(method);
        interceptorContext.setContextData(new HashMap<String, Object>());
        interceptorContext.putPrivateData(Component.class, componentView.getComponent());
        interceptorContext.putPrivateData(ComponentView.class, componentView);
        interceptorContext.putPrivateData(RemotingAttachments.class, attachments);
        return componentView.invoke(interceptorContext);
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
            outputStream.write(HEADER_MESSAGE_INVOCATION_RESPONSE);
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


}
