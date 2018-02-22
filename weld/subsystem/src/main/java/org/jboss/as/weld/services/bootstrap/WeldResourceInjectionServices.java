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

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.ee.component.EEDefaultResourceJndiNames;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.spi.ResourceInjectionResolver;
import org.jboss.as.weld.util.ResourceInjectionUtilities;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;
import org.wildfly.security.manager.WildFlySecurityManager;

public class WeldResourceInjectionServices extends AbstractResourceInjectionServices implements ResourceInjectionServices {

    private static final String USER_TRANSACTION_LOCATION = "java:comp/UserTransaction";
    private static final String USER_TRANSACTION_CLASS_NAME = "javax.transaction.UserTransaction";
    private static final String HANDLE_DELEGATE_CLASS_NAME = "javax.ejb.spi.HandleDelegate";
    private static final String TIMER_SERVICE_CLASS_NAME = "javax.ejb.TimerService";
    private static final String ORB_CLASS_NAME = "org.omg.CORBA.ORB";
    private static final String TRANSACTION_SYNC_REGISTRY_LOCATION = "java:comp/TransactionSynchronizationRegistry";
    private static final String TRANSACTION_SYNC_REGISTRY_CLASS_NAME = "javax.transaction.TransactionSynchronizationRegistry";

    private static final String EE_CONTEXT_SERVICE_CLASS_NAME = "javax.enterprise.concurrent.ContextService";
    private static final String EE_DATASOURCE_CLASS_NAME = "javax.sql.DataSource";
    private static final String EE_JMS_CONNECTION_FACTORY_CLASS_NAME = "javax.jms.ConnectionFactory";
    private static final String EE_MANAGED_EXECUTOR_SERVICE_CLASS_NAME = "javax.enterprise.concurrent.ManagedExecutorService";
    private static final String EE_MANAGED_SCHEDULED_EXECUTOR_SERVICE_CLASS_NAME = "javax.enterprise.concurrent.ManagedScheduledExecutorService";
    private static final String EE_MANAGED_THREAD_FACTORY_CLASS_NAME = "javax.enterprise.concurrent.ManagedThreadFactory";

    private static final String EJB_CONTEXT_LOCATION = "java:comp/EJBContext";
    private static final String EJB_CONTEXT_CLASS_NAME = "javax.ejb.EJBContext";
    private static final String EJB_SESSION_CONTEXT_CLASS_NAME = "javax.ejb.SessionContext";
    private static final String EJB_MESSAGE_DRIVEN_CONTEXT_CLASS_NAME = "javax.ejb.MessageDrivenContext";
    private static final String EJB_ENTITY_CONTEXT_CLASS_NAME = "javax.ejb.EntityContext";

    private static final String WEB_SERVICE_CONTEXT_CLASS_NAME = "javax.xml.ws.WebServiceContext";

    private final Context context;

    private final boolean warModule;

    private final List<ResourceInjectionResolver> resourceResolvers;

    public WeldResourceInjectionServices(final ServiceRegistry serviceRegistry, final EEModuleDescription moduleDescription, Module module, boolean warModule) {
        super(serviceRegistry, moduleDescription, module);
        this.warModule = warModule;
        try {
            this.context = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        final Iterator<ResourceInjectionResolver> resolvers = ServiceLoader.load(ResourceInjectionResolver.class,
                WildFlySecurityManager.getClassLoaderPrivileged(WeldResourceInjectionServices.class)).iterator();
        if (!resolvers.hasNext()) {
            this.resourceResolvers = Collections.emptyList();
        } else {
            this.resourceResolvers = new ArrayList<>();
            while (resolvers.hasNext()) {
                this.resourceResolvers.add(resolvers.next());
            }
        }

    }

    protected String getEJBResourceName(InjectionPoint injectionPoint, String proposedName) {
        if (injectionPoint.getType() instanceof Class<?>) {
            final Class<?> type = (Class<?>) injectionPoint.getType();
            final String typeName = type.getName();
            if (USER_TRANSACTION_CLASS_NAME.equals(typeName)) {
                return USER_TRANSACTION_LOCATION;
            } else if (HANDLE_DELEGATE_CLASS_NAME.equals(typeName)) {
                WeldLogger.ROOT_LOGGER.injectionTypeNotValue(HANDLE_DELEGATE_CLASS_NAME, injectionPoint.getMember());
                return proposedName;
            } else if (ORB_CLASS_NAME.equals(typeName)) {
                WeldLogger.ROOT_LOGGER.injectionTypeNotValue(ORB_CLASS_NAME, injectionPoint.getMember());
                return proposedName;
            } else if (TIMER_SERVICE_CLASS_NAME.equals(typeName)) {
                WeldLogger.ROOT_LOGGER.injectionTypeNotValue(TIMER_SERVICE_CLASS_NAME, injectionPoint.getMember());
                return proposedName;
            } else if (EJB_CONTEXT_CLASS_NAME.equals(typeName) ||
                    EJB_SESSION_CONTEXT_CLASS_NAME.equals(typeName) ||
                    EJB_MESSAGE_DRIVEN_CONTEXT_CLASS_NAME.equals(typeName) ||
                    EJB_ENTITY_CONTEXT_CLASS_NAME.equals(typeName)) {
                return EJB_CONTEXT_LOCATION;
            }  else if (WEB_SERVICE_CONTEXT_CLASS_NAME.equals(typeName)) {
                //horrible hack
                //there is not actually a binding we can use for this
                //the whole CDI+bindings thing will likely be reviewed in EE8
                return WEB_SERVICE_CONTEXT_CLASS_NAME;
            } else if (TRANSACTION_SYNC_REGISTRY_CLASS_NAME.equals(typeName)) {
                return TRANSACTION_SYNC_REGISTRY_LOCATION;
            } else {
                // EE default bindings
                EEDefaultResourceJndiNames eeDefaultResourceJndiNames = moduleDescription.getDefaultResourceJndiNames();
                if (eeDefaultResourceJndiNames.getContextService() != null && EE_CONTEXT_SERVICE_CLASS_NAME.equals(typeName)) {
                    return eeDefaultResourceJndiNames.getContextService();
                } else if (eeDefaultResourceJndiNames.getDataSource() != null && EE_DATASOURCE_CLASS_NAME.equals(typeName)) {
                    return eeDefaultResourceJndiNames.getDataSource();
                } else if (eeDefaultResourceJndiNames.getJmsConnectionFactory() != null && EE_JMS_CONNECTION_FACTORY_CLASS_NAME.equals(typeName)) {
                    return eeDefaultResourceJndiNames.getJmsConnectionFactory();
                } else if (eeDefaultResourceJndiNames.getManagedExecutorService() != null && EE_MANAGED_EXECUTOR_SERVICE_CLASS_NAME.equals(typeName)) {
                    return eeDefaultResourceJndiNames.getManagedExecutorService();
                } else if (eeDefaultResourceJndiNames.getManagedScheduledExecutorService() != null && EE_MANAGED_SCHEDULED_EXECUTOR_SERVICE_CLASS_NAME.equals(typeName)) {
                    return eeDefaultResourceJndiNames.getManagedScheduledExecutorService();
                } else if (eeDefaultResourceJndiNames.getManagedThreadFactory() != null && EE_MANAGED_THREAD_FACTORY_CLASS_NAME.equals(typeName)) {
                    return eeDefaultResourceJndiNames.getManagedThreadFactory();
                }
            }
        }
        return proposedName;
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
        if (isKnownNamespace(result) && injectionPoint.getAnnotated().isAnnotationPresent(Produces.class)) {
            validateResourceInjectionPointType(getManagedReferenceFactory(getBindInfo(result)), injectionPoint);
        }
        return new ResourceReferenceFactory<Object>() {
            @Override
            public ResourceReference<Object> createResource() {
                return new SimpleResourceReference<Object>(resolveResource(injectionPoint));
            }
        };
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(final String jndiName, final String mappedName) {
        return new ResourceReferenceFactory<Object>() {
            @Override
            public ResourceReference<Object> createResource() {
                return new SimpleResourceReference<Object>(resolveResource(jndiName, mappedName));
            }
        };
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
                moduleDescription.getModuleName(), !warModule, result);
    }

    @Override
    public Object resolveResource(InjectionPoint injectionPoint) {
        final Member member = injectionPoint.getMember();
        AnnotatedMember<?> annotatedMember;
        if (injectionPoint.getAnnotated() instanceof AnnotatedField) {
            annotatedMember = (AnnotatedField<?>) injectionPoint.getAnnotated();
        } else {
            annotatedMember = ((AnnotatedParameter<?>) injectionPoint.getAnnotated()).getDeclaringCallable();
        }
        if (!annotatedMember.isAnnotationPresent(Resource.class)) {
            throw WeldLogger.ROOT_LOGGER.annotationNotFound(Resource.class, member);
        }
        if (member instanceof Method && ((Method) member).getParameterTypes().length != 1) {
            throw WeldLogger.ROOT_LOGGER.injectionPointNotAJavabean((Method) member);
        }
        String name = getResourceName(injectionPoint);

        for (ResourceInjectionResolver resolver : resourceResolvers) {
            Object result = resolver.resolve(name);
            if (result != null) {
                return result;
            }
        }
        try {
            return context.lookup(name);
        } catch (NamingException e) {
            throw WeldLogger.ROOT_LOGGER.couldNotFindResource(name, injectionPoint.getMember().toString(), e);
        }
    }

    @Override
    public Object resolveResource(String jndiName, String mappedName) {
        String name = ResourceInjectionUtilities.getResourceName(jndiName, mappedName);
        try {
            return context.lookup(name);
        } catch (NamingException e) {
            throw WeldLogger.ROOT_LOGGER.couldNotFindResource(name, e);
        }
    }
}
