/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.ExcludeDefaultInterceptors;

import org.jboss.as.ee.metadata.AbstractEEAnnotationProcessor;
import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;

/**
 * Processes Jakarta Enterprise Beans annotations and attaches them to the {@link org.jboss.as.ee.component.EEModuleClassDescription}
 *
 * @author Stuart Douglas
 */
public class EEAnnotationProcessor extends AbstractEEAnnotationProcessor {

    final List<ClassAnnotationInformationFactory> factories;

    public EEAnnotationProcessor() {
        List<ClassAnnotationInformationFactory> factories = new ArrayList<ClassAnnotationInformationFactory>();
        factories.add(new InterceptorsAnnotationInformationFactory());
        factories.add(new BooleanAnnotationInformationFactory<ExcludeDefaultInterceptors>(ExcludeDefaultInterceptors.class));
        factories.add(new BooleanAnnotationInformationFactory<ExcludeClassInterceptors>(ExcludeClassInterceptors.class));
        this.factories = Collections.unmodifiableList(factories);
    }


    @Override
    protected List<ClassAnnotationInformationFactory> annotationInformationFactories() {
        return factories;
    }
}
