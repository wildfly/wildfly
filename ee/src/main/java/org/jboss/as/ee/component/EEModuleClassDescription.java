/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;

/**
 * The description of a (possibly annotated) class in an EE module.
 *
 * This class must be thread safe as it may be used by sub deployments at the same time
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleClassDescription {


    private final String className;
    private boolean invalid;
    private StringBuilder invalidMessageBuilder;
    private final Map<Class<? extends Annotation>, ClassAnnotationInformation<?,?>> annotationInformation = Collections.synchronizedMap(new HashMap<Class<? extends Annotation>, ClassAnnotationInformation<?, ?>>());
    private InterceptorClassDescription interceptorClassDescription = InterceptorClassDescription.EMPTY_INSTANCE;

    private final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();
    private final Map<InjectionTarget, ResourceInjectionConfiguration> injectionConfigurations = new HashMap<InjectionTarget, ResourceInjectionConfiguration>();

    public EEModuleClassDescription(final String className) {
        this.className = className;
    }

    /**
     * Get the class name of this EE module class.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    public InterceptorClassDescription getInterceptorClassDescription() {
        return interceptorClassDescription;
    }

    public void setInterceptorClassDescription(final InterceptorClassDescription interceptorClassDescription) {
        if(interceptorClassDescription == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("interceptorClassDescription", "module class", className);
        }
        this.interceptorClassDescription = interceptorClassDescription;
    }


    /**
     * Get the binding configurations for this EE module class.
     *
     * @return the binding configurations
     */
    public List<BindingConfiguration> getBindingConfigurations() {
        return bindingConfigurations;
    }

    /**
     * Get the resource injection configurations for this EE module class.
     *
     * @return the resource injection configuration
     */
    public Map<InjectionTarget, ResourceInjectionConfiguration> getInjectionConfigurations() {
        return injectionConfigurations;
    }

    public void addResourceInjection(final ResourceInjectionConfiguration injection) {
        injectionConfigurations.put(injection.getTarget(), injection);
    }

    public void addAnnotationInformation(ClassAnnotationInformation annotationInformation) {
        this.annotationInformation.put(annotationInformation.getAnnotationType(), annotationInformation);
    }

    public <A extends Annotation, T> ClassAnnotationInformation<A, T> getAnnotationInformation(Class<A> annotationType) {
        return (ClassAnnotationInformation<A, T>) this.annotationInformation.get(annotationType);
    }

    public synchronized void setInvalid(String message) {
        if(!invalid) {
            invalid = true;
            invalidMessageBuilder = new StringBuilder();
        } else {
            invalidMessageBuilder.append('\n');
        }
        invalidMessageBuilder.append(message);
    }

    public synchronized boolean isInvalid() {
        return invalid;
    }

    public String getInvalidMessage() {
        if(invalidMessageBuilder == null) {
            return "";
        }
        return invalidMessageBuilder.toString();
    }
}
