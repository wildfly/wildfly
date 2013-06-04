package org.jboss.as.weld.services.bootstrap;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.xml.ws.WebServiceRef;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.webservices.webserviceref.WSRefAnnotationWrapper;
import org.jboss.as.webservices.webserviceref.WebServiceReferences;
import org.jboss.weld.injection.spi.JaxwsInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

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

        WebServiceRef annotation = injectionPoint.getAnnotated().getAnnotation(WebServiceRef.class);
        if(annotation == null) {
            return null;
        }
        try {
            ManagedReferenceFactory factory = WebServiceReferences.createWebServiceFactory(deploymentUnit, classNameFromType(injectionPoint.getType()), new WSRefAnnotationWrapper(annotation), (AnnotatedElement) injectionPoint.getMember(), annotation.name());
            return new ManagedReferenceFactoryToResourceReferenceFactoryAdapter<>(factory);
        } catch (DeploymentUnitProcessingException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public void cleanup() {
        this.deploymentUnit = null;
    }
}
