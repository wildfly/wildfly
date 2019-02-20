/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.interceptors.UserInterceptorFactory;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.modules.Module;
import org.jboss.msc.value.CachedValue;
import org.jboss.msc.value.ConstructedValue;
import org.jboss.msc.value.Value;

import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.InvocationContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class ServerInterceptorsViewConfigurator implements ViewConfigurator {

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    public static final ServerInterceptorsViewConfigurator INSTANCE = new ServerInterceptorsViewConfigurator();

    private ServerInterceptorsViewConfigurator() {
    }

    @Override
    public void configure(final DeploymentPhaseContext deploymentPhaseContext, final ComponentConfiguration componentConfiguration, final ViewDescription viewDescription, final ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
        final ComponentDescription componentDescription = componentConfiguration.getComponentDescription();
        if (!(componentDescription instanceof EJBComponentDescription)) {
            return;
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
        final Set<InterceptorDescription> serverInterceptors = ejbComponentDescription.getServerInterceptors();
        if (serverInterceptors == null || serverInterceptors.isEmpty()) {
            return;
        }
        this.doConfigure(deploymentPhaseContext, ejbComponentDescription, serverInterceptors, viewConfiguration);
    }

    private void doConfigure(final DeploymentPhaseContext context, final EJBComponentDescription ejbComponentDescription, final Set<InterceptorDescription> serverInterceptors,
                             final ViewConfiguration viewConfiguration) {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final List<InterceptorFactory> serverInterceptorsAroundInvoke = new ArrayList<>();
        final List<InterceptorFactory> serverInterceptorsAroundTimeout = new ArrayList<>();
        for (final InterceptorDescription interceptorDescription : ejbComponentDescription.getServerInterceptors()) {
            final String interceptorClassName = interceptorDescription.getInterceptorClassName();
            final Class<?> interceptorClass;
            try {
                interceptorClass = ClassLoadingUtils.loadClass(interceptorClassName, module);
            } catch (ClassNotFoundException e) {
                throw EeLogger.ROOT_LOGGER.cannotLoadInterceptor(e, interceptorClassName);
            }
            final Index index = buildIndexForClass(interceptorClass);
            serverInterceptorsAroundInvoke.addAll(findAnnotatedMethods(interceptorClass, index, AroundInvoke.class));
            if(ejbComponentDescription.isTimerServiceRequired()){
                serverInterceptorsAroundTimeout.addAll(findAnnotatedMethods(interceptorClass, index, AroundTimeout.class));
            }
        }
        final List<Method> viewMethods = viewConfiguration.getProxyFactory().getCachedMethods();
        for (final Method method : viewMethods) {
            viewConfiguration.addViewInterceptor(method, new UserInterceptorFactory(weaved(serverInterceptorsAroundInvoke), weaved(serverInterceptorsAroundTimeout)), InterceptorOrder.View.USER_APP_SPECIFIC_CONTAINER_INTERCEPTORS);
        }
    }

    private Index buildIndexForClass(final Class<?> interceptorClass) {
        try {
            final String classNameAsResource = interceptorClass.getName().replaceAll("\\.", "/").concat(".class");
            final InputStream stream = interceptorClass.getClassLoader().getResourceAsStream(classNameAsResource);
            final Indexer indexer = new Indexer();
            indexer.index(stream);
            stream.close();
            return indexer.complete();
        } catch (IOException e) {
            throw EjbLogger.ROOT_LOGGER.cannotBuildIndexForServerInterceptor(interceptorClass.getName(), e);
        }
    }

    private List<InterceptorFactory> findAnnotatedMethods(final Class<?> interceptorClass, final Index index, final Class<?> annotationClass){
        final List<InterceptorFactory> interceptorFactories = new ArrayList<>();
        final DotName annotationName = DotName.createSimple(annotationClass.getName());
        final List<AnnotationInstance> annotations = index.getAnnotations(annotationName);

        for (final AnnotationInstance annotation : annotations) {
            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                final MethodInfo methodInfo = annotation.target().asMethod();
                final Constructor<?> constructor;
                try {
                    constructor = interceptorClass.getConstructor(EMPTY_CLASS_ARRAY);
                } catch (NoSuchMethodException e) {
                    throw EjbLogger.ROOT_LOGGER.serverInterceptorNoEmptyConstructor(interceptorClass.toString(), e);
                }
                try {
                    final Method annotatedMethod = interceptorClass.getMethod(methodInfo.name(), new Class[]{InvocationContext.class});
                    final InterceptorFactory interceptorFactory = createInterceptorFactoryForServerInterceptor(annotatedMethod, constructor);
                    interceptorFactories.add(interceptorFactory);
                } catch (NoSuchMethodException e) {
                    EjbLogger.ROOT_LOGGER.serverInterceptorInvalidMethod(methodInfo.name(), interceptorClass.toString(), annotationClass.toString(), e);
                }
            }
        }
        return interceptorFactories;
    }

    private static InterceptorFactory weaved(final Collection<InterceptorFactory> interceptorFactories) {
        return new InterceptorFactory() {
            @Override
            public Interceptor create(InterceptorFactoryContext context) {
                final Interceptor[] interceptors = new Interceptor[interceptorFactories.size()];
                final Iterator<InterceptorFactory> factories = interceptorFactories.iterator();
                for (int i = 0; i < interceptors.length; i++) {
                    interceptors[i] = factories.next().create(context);
                }
                return Interceptors.getWeavedInterceptor(interceptors);
            }
        };
    }

    private InterceptorFactory createInterceptorFactoryForServerInterceptor(final Method method, final Constructor interceptorConstructor) {
        // The managed reference is going to be ConstructedValue, using the container-interceptor's constructor
        final ConstructedValue interceptorInstanceValue = new ConstructedValue(interceptorConstructor, Collections.<Value<?>>emptyList());
        // we *don't* create multiple instances of the container-interceptor class, but we just reuse a single instance and it's *not*
        // tied to the EJB component instance lifecycle.
        final CachedValue cachedInterceptorInstanceValue = new CachedValue(interceptorInstanceValue);
        // ultimately create the managed reference which is backed by the CachedValue
        final ManagedReference interceptorInstanceRef = new ValueManagedReference(cachedInterceptorInstanceValue);
        // return the ContainerInterceptorMethodInterceptorFactory which is responsible for creating an Interceptor
        // which can invoke the container-interceptor's around-invoke/around-timeout methods
        return new ContainerInterceptorMethodInterceptorFactory(interceptorInstanceRef, method);
    }


}
