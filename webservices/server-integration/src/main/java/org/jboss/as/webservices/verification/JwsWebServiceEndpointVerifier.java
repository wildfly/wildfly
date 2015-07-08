/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.verification;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jws.WebMethod;

import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.logging.WSLogger;

/**
 * Verifies the structural correctness of JSR-181 style web services.
 * <ul>
 * <li>web methods must not be static or final
 * <li>web methods must be public
 * <li>web service implementation class must not declare a finalize method
 * <ul>
 * <br>
 * We don't check for the @WebService annotation or whether the class is abstract, final or public because these checks are
 * performed by {@link org.jboss.as.webservices.util.ASHelper.isJaxwsEndpoint(ClassInfo, CompositeIndex)} from {@link
 * org.jboss.as.webservices.deployers.AbstractIntegrationProcessorJAXWS.deploy(DeploymentPhaseContext)}.<br>
 *
 * @author sfcoy
 *
 */
public final class JwsWebServiceEndpointVerifier {

    private final DeploymentReflectionIndex deploymentReflectionIndex;

    private final Class<?> endpointClass;

    private final Class<?> endpointInterfaceClass;

    final List<VerificationFailure> verificationFailures = new LinkedList<VerificationFailure>();

    public JwsWebServiceEndpointVerifier(Class<?> endpointClass, Class<?> endpointInterfaceClass,
            DeploymentReflectionIndex deploymentReflectionIndex) {
        this.deploymentReflectionIndex = deploymentReflectionIndex;
        this.endpointClass = endpointClass;
        this.endpointInterfaceClass = endpointInterfaceClass;
    }

    public void verify() {
        if (endpointInterfaceClass != null) {
            for (Method endpointInterfaceDefinedWebMethod : endpointInterfaceDefinedWebMethods()) {
                verifyWebMethod(endpointInterfaceDefinedWebMethod);
            }
        } // else implicit web methods are valid by definition
        verifyFinalizeMethod();
    }

    public boolean failed() {
        return !verificationFailures.isEmpty();
    }

    public void logFailures() {
        for (VerificationFailure verificationFailure : verificationFailures)
            verificationFailure.logFailure();
    }

    void verifyWebMethod(final Method endpointInterfaceDefinedWebMethod) {
        final Method endpointImplementationMethod = findEndpointImplMethodMatching(endpointInterfaceDefinedWebMethod);
        if (endpointImplementationMethod != null) {
            final int methodModifiers = endpointImplementationMethod.getModifiers();
            final WebMethod possibleWebMethodAnnotation = endpointImplementationMethod.getAnnotation(WebMethod.class);
            if (possibleWebMethodAnnotation == null || !possibleWebMethodAnnotation.exclude()) {
                if (Modifier.isPublic(methodModifiers)) {
                    if (Modifier.isStatic(methodModifiers) || Modifier.isFinal(methodModifiers)) {
                        verificationFailures.add(new WebMethodIsStaticOrFinal(endpointImplementationMethod));
                    }
                } else {
                    verificationFailures.add(new WebMethodIsNotPublic(endpointImplementationMethod));
                }
            }
        }
    }

    void verifyFinalizeMethod() {
        ClassReflectionIndex classReflectionIndex = deploymentReflectionIndex.getClassIndex(endpointClass);
        Method finalizeMethod = classReflectionIndex.getMethod(void.class, "finalize");
        if (finalizeMethod != null) {
            verificationFailures.add(new ImplementationHasFinalize());
        }
    }

    Collection<Method> endpointInterfaceDefinedWebMethods() {
        return deploymentReflectionIndex.getClassIndex(endpointInterfaceClass).getMethods();
    }

    Method findEndpointImplMethodMatching(final Method endpointInterfaceDefinedWebMethod) {
        try {
            return endpointClass.getMethod(endpointInterfaceDefinedWebMethod.getName(),
                    endpointInterfaceDefinedWebMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            try {
                return endpointClass.getDeclaredMethod(endpointInterfaceDefinedWebMethod.getName(),
                        endpointInterfaceDefinedWebMethod.getParameterTypes());
            } catch (NoSuchMethodException e1) {
                verificationFailures.add(new WebServiceMethodNotFound(endpointInterfaceDefinedWebMethod));
            }
        } catch (SecurityException e) {
            verificationFailures.add(new WebServiceMethodNotAccessible(endpointInterfaceDefinedWebMethod, e));
        }
        return null;
    }

    abstract class VerificationFailure {
        abstract void logFailure();
    }

    abstract class MethodVerificationFailure extends VerificationFailure {

        protected final Method failedMethod;

        protected MethodVerificationFailure(Method failedMethod) {
            this.failedMethod = failedMethod;
        }

    }

    final class ImplementationHasFinalize extends VerificationFailure {

        @Override
        public void logFailure() {
            WSLogger.ROOT_LOGGER.finalizeMethodNotAllowed(endpointClass);
        }

    }

    final class WebMethodIsStaticOrFinal extends MethodVerificationFailure {

        WebMethodIsStaticOrFinal(Method failedMethod) {
            super(failedMethod);
        }

        @Override
        public void logFailure() {
            WSLogger.ROOT_LOGGER.webMethodMustNotBeStaticOrFinal(failedMethod);
        }

    }

    final class WebMethodIsNotPublic extends MethodVerificationFailure {

        WebMethodIsNotPublic(Method failedMethod) {
            super(failedMethod);
        }

        @Override
        public void logFailure() {
            WSLogger.ROOT_LOGGER.webMethodMustBePublic(failedMethod);
        }

    }

    final class WebServiceMethodNotFound extends MethodVerificationFailure {

        WebServiceMethodNotFound(Method failedMethod) {
            super(failedMethod);
        }

        @Override
        public void logFailure() {
            WSLogger.ROOT_LOGGER.webServiceMethodNotFound(endpointClass, failedMethod);
        }

    }

    final class WebServiceMethodNotAccessible extends MethodVerificationFailure {

        private final SecurityException securityException;

        WebServiceMethodNotAccessible(Method failedMethod, SecurityException e) {
            super(failedMethod);
            this.securityException = e;
        }

        @Override
        public void logFailure() {
            WSLogger.ROOT_LOGGER.accessibleWebServiceMethodNotFound(endpointClass, failedMethod, securityException);
        }

    }

}
