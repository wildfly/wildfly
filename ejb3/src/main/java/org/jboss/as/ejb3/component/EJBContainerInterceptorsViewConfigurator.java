/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ClassDescriptionTraversal;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.interceptors.UserInterceptorFactory;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.modules.Module;
import org.jboss.msc.value.CachedValue;
import org.jboss.msc.value.ConstructedValue;
import org.jboss.msc.value.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX;

/**
 * A {@link ViewConfigurator} which sets up the EJB view with the relevant {@link Interceptor}s
 * which will carry out invocation on the container-interceptor(s) applicable for an EJB, during an EJB method invocation
 *
 * @author Jaikiran Pai
 */
public class EJBContainerInterceptorsViewConfigurator implements ViewConfigurator {

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    public static final EJBContainerInterceptorsViewConfigurator INSTANCE = new EJBContainerInterceptorsViewConfigurator();

    private EJBContainerInterceptorsViewConfigurator() {
    }

    @Override
    public void configure(DeploymentPhaseContext deploymentPhaseContext, ComponentConfiguration componentConfiguration, ViewDescription viewDescription, ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
        final ComponentDescription componentDescription = componentConfiguration.getComponentDescription();
        // ideally it should always be an EJBComponentDescription when this view configurator is invoked, but let's just make sure
        if (!(componentDescription instanceof EJBComponentDescription)) {
            return;
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
        // we don't want to waste time processing if there are no container interceptors applicable for the EJB
        final Set<InterceptorDescription> allContainerInterceptors = ejbComponentDescription.getAllContainerInterceptors();
        if (allContainerInterceptors == null || allContainerInterceptors.isEmpty()) {
            return;
        }
        // do the processing
        this.doConfigure(deploymentPhaseContext, ejbComponentDescription, viewConfiguration);
    }

    private void doConfigure(final DeploymentPhaseContext context, final EJBComponentDescription ejbComponentDescription,
                             final ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);

        final Map<String, List<InterceptorFactory>> userAroundInvokesByInterceptorClass = new HashMap<String, List<InterceptorFactory>>();
        final Map<String, List<InterceptorFactory>> userAroundTimeoutsByInterceptorClass;
        if (ejbComponentDescription.isTimerServiceRequired()) {
            userAroundTimeoutsByInterceptorClass = new HashMap<String, List<InterceptorFactory>>();
        } else {
            userAroundTimeoutsByInterceptorClass = null;
        }
        // First step - find the applicable @AroundInvoke/@AroundTimeout methods on all the container-interceptors and keep track of that
        // info
        for (final InterceptorDescription interceptorDescription : ejbComponentDescription.getAllContainerInterceptors()) {
            final String interceptorClassName = interceptorDescription.getInterceptorClassName();
            final Class<?> intereptorClass;
            try {
                intereptorClass = ClassLoadingUtils.loadClass(interceptorClassName, module);
            } catch (ClassNotFoundException e) {
                throw EeLogger.ROOT_LOGGER.cannotLoadInterceptor(e, interceptorClassName);
            }
            // run the interceptor class (and its super class hierarchy) through the InterceptorClassDescriptionTraversal so that it can
            // find the relevant @AroundInvoke/@AroundTimeout methods
            final InterceptorClassDescriptionTraversal interceptorClassDescriptionTraversal = new InterceptorClassDescriptionTraversal(intereptorClass, applicationClasses, deploymentUnit, ejbComponentDescription);
            interceptorClassDescriptionTraversal.run();
            // now that the InterceptorClassDescriptionTraversal has done the relevant processing, keep track of the @AroundInvoke and
            // @AroundTimeout methods applicable for this interceptor class, within a map
            final List<InterceptorFactory> aroundInvokeInterceptorFactories = interceptorClassDescriptionTraversal.getAroundInvokeInterceptorFactories();
            if (aroundInvokeInterceptorFactories != null) {
                userAroundInvokesByInterceptorClass.put(interceptorClassName, aroundInvokeInterceptorFactories);
            }
            if (ejbComponentDescription.isTimerServiceRequired()) {
                final List<InterceptorFactory> aroundTimeoutInterceptorFactories = interceptorClassDescriptionTraversal.getAroundTimeoutInterceptorFactories();
                if (aroundTimeoutInterceptorFactories != null) {
                    userAroundTimeoutsByInterceptorClass.put(interceptorClassName, aroundTimeoutInterceptorFactories);
                }
            }
        }

        // At this point we have each interceptor class mapped against their corresponding @AroundInvoke/@AroundTimeout InterceptorFactory(s)
        // Let's now iterate over all the methods of the EJB view and apply the relevant InterceptorFactory(s) to that method
        final List<InterceptorDescription> classLevelContainerInterceptors = ejbComponentDescription.getClassLevelContainerInterceptors();
        final Map<MethodIdentifier, List<InterceptorDescription>> methodLevelContainerInterceptors = ejbComponentDescription.getMethodLevelContainerInterceptors();
        final List<Method> viewMethods = viewConfiguration.getProxyFactory().getCachedMethods();
        for (final Method method : viewMethods) {
            final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(method.getReturnType(), method.getName(), method.getParameterTypes());
            final List<InterceptorFactory> aroundInvokesApplicableForMethod = new ArrayList<InterceptorFactory>();
            final List<InterceptorFactory> aroundTimeoutsApplicableForMethod = new ArrayList<InterceptorFactory>();
            // first add the default interceptors (if not excluded) to the deque
            if (!ejbComponentDescription.isExcludeDefaultContainerInterceptors() && !ejbComponentDescription.isExcludeDefaultContainerInterceptors(methodIdentifier)) {
                for (final InterceptorDescription interceptorDescription : ejbComponentDescription.getDefaultContainerInterceptors()) {
                    String interceptorClassName = interceptorDescription.getInterceptorClassName();
                    final List<InterceptorFactory> aroundInvokesOnInterceptor = userAroundInvokesByInterceptorClass.get(interceptorClassName);
                    if (aroundInvokesOnInterceptor != null) {
                        aroundInvokesApplicableForMethod.addAll(aroundInvokesOnInterceptor);
                    }
                    if (ejbComponentDescription.isTimerServiceRequired()) {
                        final List<InterceptorFactory> aroundTimeoutsOnInterceptor = userAroundTimeoutsByInterceptorClass.get(interceptorClassName);
                        if (aroundTimeoutsOnInterceptor != null) {
                            aroundTimeoutsApplicableForMethod.addAll(aroundTimeoutsOnInterceptor);
                        }
                    }
                }
            }

            // now add class level interceptors (if not excluded) to the deque
            if (!ejbComponentDescription.isExcludeClassLevelContainerInterceptors(methodIdentifier)) {
                for (final InterceptorDescription interceptorDescription : classLevelContainerInterceptors) {
                    String interceptorClassName = interceptorDescription.getInterceptorClassName();
                    final List<InterceptorFactory> aroundInvokesOnInterceptor = userAroundInvokesByInterceptorClass.get(interceptorClassName);
                    if (aroundInvokesOnInterceptor != null) {
                        aroundInvokesApplicableForMethod.addAll(aroundInvokesOnInterceptor);
                    }
                    if (ejbComponentDescription.isTimerServiceRequired()) {
                        final List<InterceptorFactory> aroundTimeoutsOnInterceptor = userAroundTimeoutsByInterceptorClass.get(interceptorClassName);
                        if (aroundTimeoutsOnInterceptor != null) {
                            aroundTimeoutsApplicableForMethod.addAll(aroundTimeoutsOnInterceptor);
                        }
                    }
                }
            }

            // now add method level interceptors for to the deque so that they are triggered after the class interceptors
            final List<InterceptorDescription> interceptorsForMethod = methodLevelContainerInterceptors.get(methodIdentifier);
            if (interceptorsForMethod != null) {
                for (final InterceptorDescription methodLevelInterceptor : interceptorsForMethod) {
                    String interceptorClassName = methodLevelInterceptor.getInterceptorClassName();
                    final List<InterceptorFactory> aroundInvokesOnInterceptor = userAroundInvokesByInterceptorClass.get(interceptorClassName);
                    if (aroundInvokesOnInterceptor != null) {
                        aroundInvokesApplicableForMethod.addAll(aroundInvokesOnInterceptor);
                    }
                    if (ejbComponentDescription.isTimerServiceRequired()) {
                        final List<InterceptorFactory> aroundTimeoutsOnInterceptor = userAroundTimeoutsByInterceptorClass.get(interceptorClassName);
                        if (aroundTimeoutsOnInterceptor != null) {
                            aroundTimeoutsApplicableForMethod.addAll(aroundTimeoutsOnInterceptor);
                        }
                    }
                }
            }
            // apply the interceptors to the view's method.
            viewConfiguration.addViewInterceptor(method, new UserInterceptorFactory(weaved(aroundInvokesApplicableForMethod), weaved(aroundTimeoutsApplicableForMethod)), InterceptorOrder.View.USER_APP_SPECIFIC_CONTAINER_INTERCEPTORS);
        }
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

    /**
     * Traveses the interceptor class and its class hierarchy to find the aroundinvoke and aroundtimeout methods
     */
    private class InterceptorClassDescriptionTraversal extends ClassDescriptionTraversal {

        private final EEModuleDescription moduleDescription;
        private final EJBComponentDescription ejbComponentDescription;
        private final DeploymentReflectionIndex deploymentReflectionIndex;
        private final Class<?> interceptorClass;

        private final List<InterceptorFactory> aroundInvokeInterceptorFactories = new ArrayList<InterceptorFactory>();
        private final List<InterceptorFactory> aroundTimeoutInterceptorFactories = new ArrayList<InterceptorFactory>();

        InterceptorClassDescriptionTraversal(final Class<?> interceptorClass, final EEApplicationClasses applicationClasses,
                                             final DeploymentUnit deploymentUnit, final EJBComponentDescription ejbComponentDescription) {

            super(interceptorClass, applicationClasses);

            this.ejbComponentDescription = ejbComponentDescription;
            this.deploymentReflectionIndex = deploymentUnit.getAttachment(REFLECTION_INDEX);
            this.moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
            this.interceptorClass = interceptorClass;

        }

        @Override
        public void handle(final Class<?> clazz, EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
            final InterceptorClassDescription interceptorConfig;
            if (classDescription != null) {
                interceptorConfig = InterceptorClassDescription.merge(classDescription.getInterceptorClassDescription(), moduleDescription.getInterceptorClassOverride(clazz.getName()));
            } else {
                interceptorConfig = InterceptorClassDescription.merge(null, moduleDescription.getInterceptorClassOverride(clazz.getName()));
            }
            // get the container-interceptor class' constructor
            final ClassReflectionIndex interceptorClassReflectionIndex = deploymentReflectionIndex.getClassIndex(interceptorClass);
            final Constructor<?> interceptorClassConstructor = interceptorClassReflectionIndex.getConstructor(EMPTY_CLASS_ARRAY);
            if (interceptorClassConstructor == null) {
                throw EeLogger.ROOT_LOGGER.defaultConstructorNotFound(interceptorClass);
            }

            final MethodIdentifier aroundInvokeMethodIdentifier = interceptorConfig.getAroundInvoke();
            final InterceptorFactory aroundInvokeInterceptorFactory = createInterceptorFactory(clazz, aroundInvokeMethodIdentifier, interceptorClassConstructor);
            if (aroundInvokeInterceptorFactory != null) {
                this.aroundInvokeInterceptorFactories.add(aroundInvokeInterceptorFactory);
            }

            if (ejbComponentDescription.isTimerServiceRequired()) {
                final MethodIdentifier aroundTimeoutMethodIdentifier = interceptorConfig.getAroundTimeout();
                final InterceptorFactory aroundTimeoutInterceptorFactory = createInterceptorFactory(clazz, aroundTimeoutMethodIdentifier, interceptorClassConstructor);
                if (aroundTimeoutInterceptorFactory != null) {
                    this.aroundTimeoutInterceptorFactories.add(aroundTimeoutInterceptorFactory);
                }
            }

        }

        private InterceptorFactory createInterceptorFactory(final Class<?> clazz, final MethodIdentifier methodIdentifier, final Constructor<?> interceptorClassConstructor) throws DeploymentUnitProcessingException {
            if (methodIdentifier == null) {
                return null;
            }
            final Method method = ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, clazz, methodIdentifier);
            if (isNotOverriden(clazz, method, this.interceptorClass, deploymentReflectionIndex)) {
                return this.createInterceptorFactoryForContainerInterceptor(method, interceptorClassConstructor);
            }
            return null;
        }

        private boolean isNotOverriden(final Class<?> clazz, final Method method, final Class<?> actualClass, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
            return Modifier.isPrivate(method.getModifiers()) || ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, actualClass, method).getDeclaringClass() == clazz;
        }

        private List<InterceptorFactory> getAroundInvokeInterceptorFactories() {
            return this.aroundInvokeInterceptorFactories;
        }

        private List<InterceptorFactory> getAroundTimeoutInterceptorFactories() {
            return this.aroundTimeoutInterceptorFactories;
        }

        private InterceptorFactory createInterceptorFactoryForContainerInterceptor(final Method method, final Constructor interceptorConstructor) {
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

}
