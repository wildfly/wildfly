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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

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
    private final Deque<ClassConfigurator> configurators = new LinkedBlockingDeque<ClassConfigurator>();
    private boolean invalid;
    private StringBuilder invalidMessageBuilder;
    private final Map<Class<? extends Annotation>, ClassAnnotationInformation<?,?>> annotationInformation = Collections.synchronizedMap(new HashMap<Class<? extends Annotation>, ClassAnnotationInformation<?, ?>>());
    private InterceptorClassDescription interceptorClassDescription = InterceptorClassDescription.EMPTY_INSTANCE;

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
            throw new IllegalArgumentException("InterceptorClassDescription cannot be null");
        }
        this.interceptorClassDescription = interceptorClassDescription;
    }

    /**
     * Get the configurators for this class.
     *
     * @return the configurators
     */
    public Deque<ClassConfigurator> getConfigurators() {
        return configurators;
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
