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

package org.jboss.as.webservices.deployers;

import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;
import static org.jboss.as.webservices.WSMessages.MESSAGES;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXWS_ENDPOINTS_KEY;

import javax.jws.WebService;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.metadata.model.AbstractEndpoint;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.verification.JwsWebServiceEndpointVerifier;
import org.jboss.modules.Module;

/**
 * @author sfcoy
 *
 */
public class WSClassVerificationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final JAXWSDeployment wsDeployment = unit.getAttachment(JAXWS_ENDPOINTS_KEY);
        if (wsDeployment != null) {
            final Module module = unit.getAttachment(Attachments.MODULE);
            final DeploymentReflectionIndex deploymentReflectionIndex = unit
                    .getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
            final ClassLoader moduleClassLoader = module.getClassLoader();
            for (AbstractEndpoint pojoEndpoint : wsDeployment.getPojoEndpoints()) {
                verifyEndpoint(pojoEndpoint, moduleClassLoader, deploymentReflectionIndex);
            }
            for (AbstractEndpoint ejbEndpoint : wsDeployment.getEjbEndpoints()) {
                verifyEndpoint(ejbEndpoint, moduleClassLoader, deploymentReflectionIndex);
            }
        }
    }

    private void verifyEndpoint(final AbstractEndpoint pojoEndpoint, final ClassLoader moduleClassLoader,
            final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        ROOT_LOGGER.tracef("Verifying web service endpoint class %s", pojoEndpoint.getClassName());
        try {
            final Class<?> endpointClass = moduleClassLoader.loadClass(pojoEndpoint.getClassName());
            final WebService webServiceAnnotation = endpointClass.getAnnotation(WebService.class);
            if (webServiceAnnotation != null) {
                verifyJwsEndpoint(endpointClass, webServiceAnnotation, moduleClassLoader, deploymentReflectionIndex);
            } // otherwise it's probably a javax.xml.ws.Provider implementation
        } catch (ClassNotFoundException e) {
            throw MESSAGES.endpointClassNotFound(pojoEndpoint.getClassName());
        }
    }

    void verifyJwsEndpoint(final Class<?> endpointClass, final WebService webServiceAnnotation,
            final ClassLoader moduleClassLoader, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        final String endpointInterfaceClassName = webServiceAnnotation.endpointInterface();
        try {
            final Class<?> endpointInterfaceClass = endpointInterfaceClassName.length() > 0 ? moduleClassLoader
                    .loadClass(endpointInterfaceClassName) : null;
            final JwsWebServiceEndpointVerifier wsEndpointVerifier = new JwsWebServiceEndpointVerifier(
                    endpointClass, endpointInterfaceClass, deploymentReflectionIndex);
            wsEndpointVerifier.verify();
            if (wsEndpointVerifier.failed()) {
                wsEndpointVerifier.logFailures();
                throw MESSAGES.jwsWebServiceClassVerificationFailed(endpointClass);
            }
        } catch (ClassNotFoundException e) {
            throw MESSAGES.declaredEndpointInterfaceClassNotFound(endpointInterfaceClassName, endpointClass);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
