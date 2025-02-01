/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.deployers;

import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_PROVIDER_ANNOTATION;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXWS_ENDPOINTS_KEY;

import java.util.HashSet;
import java.util.Set;

import jakarta.jws.WebService;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.metadata.model.AbstractEndpoint;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.verification.JwsWebServiceEndpointVerifier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;

/**
 * @author sfcoy
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 *
 */
public class WSClassVerificationProcessor implements DeploymentUnitProcessor {

    private static final Set<String> cxfExportingModules = new HashSet<>();
    static {
        cxfExportingModules.add("org.apache.cxf");
        cxfExportingModules.add("org.apache.cxf.impl");
        cxfExportingModules.add("org.jboss.ws.cxf.jbossws-cxf-client");
    }

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
            if (!hasCxfModuleDependency(unit)) {
                checkCXFModuleDependency(unit);
            }
        }
    }

    private void verifyEndpoint(final AbstractEndpoint pojoEndpoint, final ClassLoader moduleClassLoader,
            final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        if (WSLogger.ROOT_LOGGER.isTraceEnabled()) {
            WSLogger.ROOT_LOGGER.tracef("Verifying web service endpoint class %s", pojoEndpoint.getClassName());
        }
        try {
            final Class<?> endpointClass = moduleClassLoader.loadClass(pojoEndpoint.getClassName());
            final WebService webServiceAnnotation = endpointClass.getAnnotation(WebService.class);
            if (webServiceAnnotation != null) {
                verifyJwsEndpoint(endpointClass, webServiceAnnotation, moduleClassLoader, deploymentReflectionIndex);
            } // otherwise it's probably a jakarta.xml.ws.Provider implementation
        } catch (ClassNotFoundException e) {
            throw WSLogger.ROOT_LOGGER.endpointClassNotFound(pojoEndpoint.getClassName());
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
                throw WSLogger.ROOT_LOGGER.jwsWebServiceClassVerificationFailed(endpointClass);
            }
        } catch (ClassNotFoundException e) {
            throw WSLogger.ROOT_LOGGER.declaredEndpointInterfaceClassNotFound(endpointInterfaceClassName, endpointClass);
        }
    }
    static boolean hasCxfModuleDependency(DeploymentUnit unit) {
        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        for (ModuleDependency dep : moduleSpec.getUserDependenciesSet()) {
            final String id = dep.getDependencyModule();
            if (cxfExportingModules.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private void checkCXFModuleDependency(DeploymentUnit unit) {
        final CompositeIndex index = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        final DotName[] dotNames = {WEB_SERVICE_ANNOTATION, WEB_SERVICE_PROVIDER_ANNOTATION};
        for (final DotName dotName : dotNames) {
            for (AnnotationInstance ai : index.getAnnotations(dotName)) {
                AnnotationTarget at = ai.target();
                if (at instanceof ClassInfo) {
                    final ClassInfo clazz = (ClassInfo)ai.target();
                    for (DotName dn : clazz.annotationsMap().keySet()) {
                        if (dn.toString().startsWith("org.apache.cxf")) {
                            WSLogger.ROOT_LOGGER.checkModuleDependency(dn.toString(), clazz.name().toString(), "org.apache.cxf");
                            return;
                        }
                    }
                }
            }
        }
    }
}
