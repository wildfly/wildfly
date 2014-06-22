/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.services.bootstrap;

import static org.jboss.as.weld.util.ResourceInjectionUtilities.getResourceAnnotated;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.jws.WebService;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.webservices.webserviceref.WSRefAnnotationWrapper;
import org.jboss.as.webservices.webserviceref.WebServiceReferences;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.util.ResourceInjectionUtilities;
import org.jboss.weld.injection.spi.JaxwsInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.util.reflection.Reflections;

/**
 * @author Stuart Douglas
 */
public class WeldJaxwsInjectionServices implements JaxwsInjectionServices {

    private DeploymentUnit deploymentUnit;

    public WeldJaxwsInjectionServices(final DeploymentUnit unit) {
        this.deploymentUnit = unit;
    }

    @Override
    public <T> ResourceReferenceFactory<T> registerWebServiceRefInjectionPoint(final InjectionPoint injectionPoint) {

        WebServiceRef annotation = getResourceAnnotated(injectionPoint).getAnnotation(WebServiceRef.class);
        if (annotation == null) {
            throw WeldLogger.ROOT_LOGGER.annotationNotFound(WebServiceRef.class, injectionPoint.getMember());
        }
        validateWebServiceRefInjectionPoint(injectionPoint, annotation);
        try {
            ManagedReferenceFactory factory = WebServiceReferences.createWebServiceFactory(deploymentUnit, classNameFromType(injectionPoint.getType()), new WSRefAnnotationWrapper(annotation), (AnnotatedElement) injectionPoint.getMember(), getBindingName(injectionPoint, annotation));
            return new ManagedReferenceFactoryToResourceReferenceFactoryAdapter<>(factory);
        } catch (DeploymentUnitProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getBindingName(final InjectionPoint injectionPoint, WebServiceRef annotation) {
        if (!annotation.name().isEmpty()) {
            return annotation.name();
        }
        return injectionPoint.getMember().getDeclaringClass().getName() + "/" + ResourceInjectionUtilities.getPropertyName(injectionPoint.getMember());
    }

    private String classNameFromType(final Type type) {
       if(type instanceof Class) {
           return ((Class) type).getName();
       } else if(type instanceof ParameterizedType) {
           return classNameFromType(((ParameterizedType) type).getRawType());
       } else {
           return type.toString();
       }
    }

    private void validateWebServiceRefInjectionPoint(InjectionPoint ip, WebServiceRef annotation) {
        Class<?> rawType = Reflections.getRawType(ip.getType());
        if (Service.class.isAssignableFrom(rawType)) {
            return;
        }
        if (!rawType.isAnnotationPresent(WebService.class)) {
            throw BeanLogger.LOG.invalidResourceProducerType(ip.getAnnotated(), annotation.value());
        }
    }

    @Override
    public void cleanup() {
        this.deploymentUnit = null;
    }
}
