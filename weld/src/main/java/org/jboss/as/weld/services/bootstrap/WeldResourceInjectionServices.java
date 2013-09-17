/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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

import java.lang.reflect.Method;

import javax.annotation.Resource;
import javax.ejb.TimerService;
import javax.ejb.spi.HandleDelegate;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.WeldMessages;
import org.jboss.as.weld.util.ResourceInjectionUtilities;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;

public class WeldResourceInjectionServices extends AbstractResourceInjectionServices implements ResourceInjectionServices {

    private static final String USER_TRANSACTION_LOCATION = "java:comp/UserTransaction";
    private static final String USER_TRANSACTION_CLASS_NAME = "javax.transaction.UserTransaction";
    private static final String HANDLE_DELEGATE_CLASS_NAME = "javax.ejb.spi.HandleDelegate";
    private static final String TIMER_SERVICE_CLASS_NAME = "javax.ejb.TimerService";
    private static final String ORB_CLASS_NAME = "org.omg.CORBA.ORB";

    private final Context context;

    protected static String getEJBResourceName(InjectionPoint injectionPoint, String proposedName) {
        if (injectionPoint.getType() instanceof Class<?>) {
            Class<?> type = (Class<?>) injectionPoint.getType();
            if (USER_TRANSACTION_CLASS_NAME.equals(type.getName())) {
                return USER_TRANSACTION_LOCATION;
            } else if (HANDLE_DELEGATE_CLASS_NAME.equals(type.getName())) {
                WeldLogger.ROOT_LOGGER.injectionTypeNotValue(HandleDelegate.class, injectionPoint.getMember());
                return proposedName;
            } else if (ORB_CLASS_NAME.equals(type.getName())) {
                WeldLogger.ROOT_LOGGER.injectionTypeNotValue(org.omg.CORBA.ORB.class, injectionPoint.getMember());
                return proposedName;
            } else if (TIMER_SERVICE_CLASS_NAME.equals(type.getName())) {
                WeldLogger.ROOT_LOGGER.injectionTypeNotValue(TimerService.class, injectionPoint.getMember());
                return proposedName;
            } else if (ContextService.class.getName().equals(type.getName())) {
                return ConcurrentServiceNames.DEFAULT_CONTEXT_SERVICE_JNDI_NAME;
            }  else if (ManagedExecutorService.class.getName().equals(type.getName())) {
                return ConcurrentServiceNames.DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME;
            }  else if (ManagedScheduledExecutorService.class.getName().equals(type.getName())) {
                return ConcurrentServiceNames.DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME;
            }  else if (ManagedThreadFactory.class.getName().equals(type.getName())) {
                return ConcurrentServiceNames.DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME;
            }
        }
        return proposedName;
    }

    public WeldResourceInjectionServices(final ServiceRegistry serviceRegistry, final EEModuleDescription moduleDescription) {
        super(serviceRegistry, moduleDescription);
        try {
            this.context = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getResourceName(InjectionPoint injectionPoint) {
        Resource resource = getResourceAnnotated(injectionPoint).getAnnotation(Resource.class);
        String mappedName = resource.mappedName();
        String lookup = resource.lookup();
        if (!lookup.isEmpty()) {
            return lookup;
        }
        if (!mappedName.isEmpty()) {
            return mappedName;
        }
        String proposedName = ResourceInjectionUtilities.getResourceName(injectionPoint);
        return getEJBResourceName(injectionPoint, proposedName);
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(final InjectionPoint injectionPoint) {
        final String result = getResourceName(injectionPoint);
        if (isKnownNamespace(result)) {
            return handleServiceLookup(result, injectionPoint);
        } else {

            return new ResourceReferenceFactory<Object>() {
                @Override
                public ResourceReference<Object> createResource() {
                    return new SimpleResourceReference<Object>(resolveResource(injectionPoint));
                }
            };
        }
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(final String jndiName, final String mappedName) {
        final String result = ResourceInjectionUtilities.getResourceName(jndiName, mappedName);
        if (isKnownNamespace(result)) {
            return handleServiceLookup(result, null);
        } else {

            return new ResourceReferenceFactory<Object>() {
                @Override
                public ResourceReference<Object> createResource() {
                    return new SimpleResourceReference<Object>(resolveResource(jndiName, mappedName));
                }
            };
        }
    }

    private boolean isKnownNamespace(String name) {
        return name.startsWith("java:global") || name.startsWith("java:app") || name.startsWith("java:module")
                || name.startsWith("java:comp") || name.startsWith("java:jboss");
    }

    @Override
    public void cleanup() {
    }

    @Override
    protected BindInfo getBindInfo(String result) {
        return ContextNames.bindInfoForEnvEntry(moduleDescription.getApplicationName(), moduleDescription.getModuleName(),
                moduleDescription.getModuleName(), false, result);
    }

    @Override
    public Object resolveResource(InjectionPoint injectionPoint) {
        if (!injectionPoint.getAnnotated().isAnnotationPresent(Resource.class)) {
            throw WeldMessages.MESSAGES.annotationNotFound(Resource.class, injectionPoint.getMember());
        }
        if (injectionPoint.getMember() instanceof Method && ((Method) injectionPoint.getMember()).getParameterTypes().length != 1) {
            throw WeldMessages.MESSAGES.injectionPointNotAJavabean((Method) injectionPoint.getMember());
        }
        String name = getResourceName(injectionPoint);
        try {
            return context.lookup(name);
        } catch (NamingException e) {
            throw WeldMessages.MESSAGES.coundNotFindResource(name, e);
        }
    }

    @Override
    public Object resolveResource(String jndiName, String mappedName) {
        String name = ResourceInjectionUtilities.getResourceName(jndiName, mappedName);
        try {
            return context.lookup(name);
        } catch (NamingException e) {
            throw WeldMessages.MESSAGES.coundNotFindResource(name, e);
        }
    }
}
