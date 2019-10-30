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

    public boolean isInvalid() {
        return invalid;
    }

    public String getInvalidMessage() {
        if(invalidMessageBuilder == null) {
            return "";
        }
        return invalidMessageBuilder.toString();
    }
}
