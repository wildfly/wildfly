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

package org.jboss.as.weld;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.NoSuchEJBException;

import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * Date: 05.11.2011
 *
 * 16051-16099
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Stuart Douglas
 */
@MessageBundle(projectCode = "JBAS")
public interface WeldMessages {

    /**
     * The messages
     */
    WeldMessages MESSAGES = Messages.getBundle(WeldMessages.class);

    @Message(id= 16051, value = "Could get beans.xml file as URL when processing file: %s")
    DeploymentUnitProcessingException couldNotGetBeansXmlAsURL(String beansXml, @Cause Throwable cause);


    @Message(id= 16052, value = "Could not load interceptor class : %s")
    DeploymentUnitProcessingException couldNotLoadInterceptorClass(String interceptorClass, @Cause Throwable cause);

    @Message(id=16053, value = "Service class %s didn't implement the javax.enterprise.inject.spi.Extension interface")
    DeploymentUnitProcessingException extensionDoesNotImplementExtension(String className, @Cause Throwable throwable);

    @Message(id = 16054, value = "View of type %s not found on EJB %s")
    IllegalArgumentException viewNotFoundOnEJB(String viewType, String ejb);

    @Message(id = 16055, value = "EJB has been removed")
    NoSuchEJBException ejbHashBeenRemoved();

    @Message(id = 16056, value = "Failed to perform CDI injection of field: %s on %s")
    RuntimeException couldNotInjectField(Field field, Class<?> beanClass, @Cause Throwable cause);

    @Message(id = 16057, value = "Failed to perform CDI injection of method: %s on %s")
    RuntimeException couldNotInjectMethod(Method method, Class<?> beanClass, @Cause Throwable cause);

    @Message(id = 16058, value = "Class %s has more that one constructor annotated with @Inject")
    RuntimeException moreThanOneBeanConstructor(Class<?> beanClass);

    @Message(id = 16059, value = "Component %s is attempting to inject the InjectionPoint into a field: %s")
    RuntimeException attemptingToInjectInjectionPointIntoField(Class clazz, Field field);

    @Message(id = 16060, value = "Could not resolve CDI bean for injection point %s with qualifiers %s")
    RuntimeException couldNotResolveInjectionPoint(String injectionPoint, Set<Annotation> qualifier);

    @Message(id = 16061, value = "Component %s is attempting to inject the InjectionPoint into a method on a component that is not a CDI bean %s")
    RuntimeException attemptingToInjectInjectionPointIntoNonBean(Class<?> componentClass, Method injectionPoint);

    @Message(id = 16062, value = "Unknown interceptor class for CDI injection %s" )
    IllegalArgumentException unknownInterceptorClassForCDIInjection(Class<?> interceptorClass);

    @Message(id = 16063, value = "%s cannot be null")
    IllegalArgumentException parameterCannotBeNull(String param);

    @Message(id = 16064, value = "Injection point represents a method which doesn't follow JavaBean conventions (must have exactly one parameter) %s")
    IllegalArgumentException injectionPointNotAJavabean(Method method);

    @Message(id = 16065, value = "%s annotation not found on %s")
    IllegalArgumentException annotationNotFound(Class<? extends Annotation> type,  Member member);

    @Message(id = 16066, value = "Could not resolve @EJB injection for %s on %s")
    IllegalArgumentException ejbNotResolved(EJB ejb, Member member);

    @Message(id = 16067, value = "Resolved more than one EJB for @EJB injection of %s on %s. Found %s")
    IllegalArgumentException moreThanOneEjbResolved(EJB ejb, Member member, final Set<ViewDescription> viewService);

    @Message(id = 16068, value = "Could not determine bean class from injection point type of %s")
    IllegalArgumentException couldNotDetermineUnderlyingType(Type type);

    @Message(id = 16069, value = "Error injecting persistence unit into CDI managed bean. Can't find a persistence unit named %s in deployment %s")
    IllegalArgumentException couldNotFindPersistenceUnit(String unitName, String deployment);

    @Message(id = 16070, value = "Could not inject SecurityManager, security is not enabled")
    IllegalStateException securityNotEnabled();

    @Message(id = 16071, value = "Singleton not set for %s. This means that you are trying to access a weld deployment with a Thread Context ClassLoader that is not associated with the deployment.")
    IllegalStateException singletonNotSet(ClassLoader classLoader);

    @Message(id = 16072, value = "%s is already running")
    IllegalStateException alreadyRunning(String object);

    @Message(id = 16073, value = "%s is not started")
    IllegalStateException notStarted(String object);

    @Message(id = 16074, value = "services cannot be added after weld has started")
    IllegalStateException cannotAddServicesAfterStart();

    @Message(id = 16075, value = "BeanDeploymentArchive with id %s not found in deployment")
    IllegalArgumentException beanDeploymentNotFound(String beanDeploymentId);
}
