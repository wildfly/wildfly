/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.interceptor.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.ejb3.component.ContainerInterceptorMethodInterceptorFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class ServerInterceptorCache {

    private final List<ServerInterceptorMetaData> serverInterceptorMetaData;

    private List<InterceptorFactory> serverInterceptorsAroundInvoke = null;
    private List<InterceptorFactory> serverInterceptorsAroundTimeout = null;

    public ServerInterceptorCache(final List<ServerInterceptorMetaData> interceptorsMetaData){
        this.serverInterceptorMetaData = interceptorsMetaData;
    }

    public List<InterceptorFactory> getServerInterceptorsAroundInvoke() {
        synchronized(this) {
            if (serverInterceptorsAroundInvoke == null) {
                loadServerInterceptors();
            }
        }
        return serverInterceptorsAroundInvoke;
    }

    public List<InterceptorFactory> getServerInterceptorsAroundTimeout() {
        synchronized(this) {
            if (serverInterceptorsAroundTimeout == null) {
                loadServerInterceptors();
            }
        }
        return serverInterceptorsAroundTimeout;
    }

    private void loadServerInterceptors(){
        serverInterceptorsAroundInvoke = new ArrayList<>();
        serverInterceptorsAroundTimeout = new ArrayList<>();
        for (final ServerInterceptorMetaData si: serverInterceptorMetaData) {
            final Class<?> interceptorClass;
            final String moduleId = si.getModule();
            try {
                final Module module = Module.getCallerModuleLoader().loadModule(moduleId);
                interceptorClass = ClassLoadingUtils.loadClass(si.getClazz(), module);
            } catch (ModuleLoadException e) {
                throw EjbLogger.ROOT_LOGGER.cannotLoadServerInterceptorModule(moduleId, e);
            } catch (ClassNotFoundException e) {
                throw EeLogger.ROOT_LOGGER.cannotLoadInterceptor(e, si.getClazz());
            }
            final Index index = buildIndexForClass(interceptorClass);
            serverInterceptorsAroundInvoke.addAll(findAnnotatedMethods(interceptorClass, index, AroundInvoke.class));
            serverInterceptorsAroundTimeout.addAll(findAnnotatedMethods(interceptorClass, index, AroundTimeout.class));
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
                    constructor = interceptorClass.getConstructor();
                } catch (NoSuchMethodException e) {
                    throw EjbLogger.ROOT_LOGGER.serverInterceptorNoEmptyConstructor(interceptorClass.toString(), e);
                }
                try {
                    final Method annotatedMethod = interceptorClass.getMethod(methodInfo.name(), new Class[]{InvocationContext.class});
                    final InterceptorFactory interceptorFactory = createInterceptorFactoryForServerInterceptor(annotatedMethod, constructor);
                    interceptorFactories.add(interceptorFactory);
                } catch (NoSuchMethodException e) {
                    throw EjbLogger.ROOT_LOGGER.serverInterceptorInvalidMethod(methodInfo.name(), interceptorClass.toString(), annotationClass.toString(), e);
                }
            }
        }
        return interceptorFactories;
    }

    private InterceptorFactory createInterceptorFactoryForServerInterceptor(final Method method, final Constructor interceptorConstructor) {
        // we *don't* create multiple instances of the container-interceptor class, but we just reuse a single instance and it's *not*
        // tied to the Jakarta Enterprise Beans component instance lifecycle.
        final ManagedReference interceptorInstanceRef = new ValueManagedReference(newInstance(interceptorConstructor));
        // return the ContainerInterceptorMethodInterceptorFactory which is responsible for creating an Interceptor
        // which can invoke the container-interceptor's around-invoke/around-timeout methods
        return new ContainerInterceptorMethodInterceptorFactory(interceptorInstanceRef, method);
    }

    private static Object newInstance(final Constructor ctor) {
        try {
            return ctor.newInstance(new Object[] {});
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}
