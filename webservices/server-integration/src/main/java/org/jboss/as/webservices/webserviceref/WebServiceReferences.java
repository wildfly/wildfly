/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.webserviceref;

import static org.jboss.as.webservices.webserviceref.WSRefUtils.processAnnotatedElement;

import java.lang.reflect.AnnotatedElement;

import javax.xml.ws.Service;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.VirtualFileAdaptor;
import org.jboss.modules.Module;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Utility class that encapsulates the creation of web service ref factories.
 * <p/>
 * This is also used by Weld to perform WS injection.
 *
 * @author Stuart Douglas
 */
public class WebServiceReferences {


    public static ManagedReferenceFactory createWebServiceFactory(final DeploymentUnit deploymentUnit, final String targetType, final WSRefAnnotationWrapper wsRefDescription, final AnnotatedElement target, String bindingName) throws DeploymentUnitProcessingException {
        final UnifiedServiceRefMetaData serviceRefUMDM = createServiceRef(deploymentUnit, targetType, wsRefDescription, target, bindingName, bindingName);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        return new WebServiceManagedReferenceFactory(serviceRefUMDM, module.getClassLoader());
    }

    public static ManagedReferenceFactory createWebServiceFactory(final DeploymentUnit deploymentUnit, final String targetType, final WSRefAnnotationWrapper wsRefDescription, final AnnotatedElement target, String bindingName, final String refKey) throws DeploymentUnitProcessingException {
        final UnifiedServiceRefMetaData serviceRefUMDM = createServiceRef(deploymentUnit, targetType, wsRefDescription, target, bindingName, refKey);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        return new WebServiceManagedReferenceFactory(serviceRefUMDM, module.getClassLoader());
    }

    private static UnifiedServiceRefMetaData createServiceRef(final DeploymentUnit unit, final String type, final WSRefAnnotationWrapper annotation, final AnnotatedElement annotatedElement, final String bindingName, final String refKey) throws DeploymentUnitProcessingException {
        final WSRefRegistry wsRefRegistry = ASHelper.getWSRefRegistry(unit);
        UnifiedServiceRefMetaData serviceRefUMDM = wsRefRegistry.get(refKey);
        if (serviceRefUMDM == null) {
            serviceRefUMDM = new UnifiedServiceRefMetaData(getUnifiedVirtualFile(unit), bindingName);
            wsRefRegistry.add(refKey, serviceRefUMDM);
        }
        initServiceRef(unit, serviceRefUMDM, type, annotation);
        processWSFeatures(serviceRefUMDM, annotatedElement);
        return serviceRefUMDM;
    }

    private static void processWSFeatures(final UnifiedServiceRefMetaData serviceRefUMDM, final AnnotatedElement annotatedElement) throws DeploymentUnitProcessingException {
        processAnnotatedElement(annotatedElement, serviceRefUMDM);
    }

    private static UnifiedServiceRefMetaData initServiceRef(final DeploymentUnit unit, final UnifiedServiceRefMetaData serviceRefUMDM, final String type, final WSRefAnnotationWrapper annotation) throws DeploymentUnitProcessingException {
        // wsdl location
        if (!isEmpty(annotation.wsdlLocation())) {
            serviceRefUMDM.setWsdlFile(annotation.wsdlLocation());
        }
        // ref class type
        final Module module = unit.getAttachment(Attachments.MODULE);
        final Class<?> typeClass = getClass(module, type);
        serviceRefUMDM.setServiceRefType(typeClass.getName());
        // ref service interface
        if (!isEmpty(annotation.value())) {
            serviceRefUMDM.setServiceInterface(annotation.value());
        } else if (Service.class.isAssignableFrom(typeClass)) {
            serviceRefUMDM.setServiceInterface(typeClass.getName());
        } else {
            serviceRefUMDM.setServiceInterface(Service.class.getName());
        }

        return serviceRefUMDM;
    }

    private static Class<?> getClass(final Module module, final String className) throws DeploymentUnitProcessingException { // TODO: refactor to common code
        final ClassLoader oldCL = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            if (!isEmpty(className)) {
                try {
                    return module.getClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException(e);
                }
            }

            return null;
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldCL);
        }
    }

    private static UnifiedVirtualFile getUnifiedVirtualFile(final DeploymentUnit unit) {
        final ResourceRoot resourceRoot = unit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        return new VirtualFileAdaptor(resourceRoot.getRoot());
    }

    private static boolean isEmpty(final String string) {
        return string == null || string.isEmpty();
    }

    private WebServiceReferences() {

    }

}
