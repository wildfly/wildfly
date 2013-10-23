/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.injection;

import javax.enterprise.inject.spi.Bean;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.injection.producer.BasicInjectionTarget;
import org.jboss.weld.injection.producer.ConstructorInterceptionInstantiator;
import org.jboss.weld.injection.producer.DefaultInstantiator;
import org.jboss.weld.injection.producer.InterceptionModelInitializer;
import org.jboss.weld.injection.producer.InterceptorApplyingInstantiator;
import org.jboss.weld.injection.producer.SubclassedComponentInstantiator;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * {@link javax.enterprise.inject.spi.InjectionTarget} implementation used for non-contextual EE components such as
 * servlets, filters, web socket endpoints, ...
 *
 * This {@link javax.enterprise.inject.spi.InjectionTarget} implementation does not provide resource injection as it would
 * otherwise be performed twice.
 *
 * Interception support is provided to instances by Weld.
 *
 * @author Jozef Hartinger
 *
 * @param <T>
 */
public class InterceptedNonContextualComponentInjectionTarget<T> extends BasicInjectionTarget<T> {

    public InterceptedNonContextualComponentInjectionTarget(EnhancedAnnotatedType<T> type, Bean<T> bean, BeanManagerImpl beanManager) {
        super(type, bean, beanManager);
        initializeInterceptors(type);
    }

    public void initializeInterceptors(EnhancedAnnotatedType<T> annotatedType) {
        initializeInterceptionModel(annotatedType);

        InterceptionModel<ClassMetadata<?>> interceptionModel = beanManager.getInterceptorModelRegistry().get(getType());
        boolean hasNonConstructorInterceptors = interceptionModel != null
                && (interceptionModel.hasExternalNonConstructorInterceptors() || interceptionModel.hasTargetClassInterceptors());

        if (hasNonConstructorInterceptors) {
            DefaultInstantiator<T> delegate = (DefaultInstantiator<T>) getInstantiator();
            setInstantiator(new SubclassedComponentInstantiator<T>(annotatedType, getBean(), delegate, beanManager));
            if (hasNonConstructorInterceptors) {
                setInstantiator(new InterceptorApplyingInstantiator<T>(getInstantiator(), interceptionModel, getType()));
            }
        }

        setupConstructorInterceptionInstantiator(interceptionModel);
    }

    private void initializeInterceptionModel(EnhancedAnnotatedType<T> annotatedType) {
        DefaultInstantiator<T> instantiator = (DefaultInstantiator<T>) getInstantiator();
        if (!beanManager.getInterceptorModelRegistry().containsKey(getType())) {
            new InterceptionModelInitializer<T>(beanManager, annotatedType, instantiator.getConstructorInjectionPoint().getAnnotated(), getBean()).init();
        }
    }

    private void setupConstructorInterceptionInstantiator(InterceptionModel<ClassMetadata<?>> interceptionModel) {
        if (interceptionModel != null && interceptionModel.hasExternalConstructorInterceptors()) {
            setInstantiator(new ConstructorInterceptionInstantiator<T>(getInstantiator(), interceptionModel, getType()));
        }
    }

}
