/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.ejb.Timeout;

import org.jboss.as.ee.component.deployers.BooleanAnnotationInformationFactory;
import org.jboss.as.ee.metadata.AbstractEEAnnotationProcessor;
import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;

/**
 * Processes EJB annotations and attaches them to the {@link org.jboss.as.ee.component.EEModuleClassDescription}
 *
 * @author Stuart Douglas
 */
public class TimerServiceAnnotationProcessor extends AbstractEEAnnotationProcessor {

    final List<ClassAnnotationInformationFactory> factories;

    public TimerServiceAnnotationProcessor() {
        final List<ClassAnnotationInformationFactory> factories = new ArrayList<ClassAnnotationInformationFactory>();
        factories.add(new BooleanAnnotationInformationFactory<Timeout>(Timeout.class));
        factories.add(new ScheduleAnnotationInformationFactory());
        this.factories = Collections.unmodifiableList(factories);
    }


    @Override
    protected List<ClassAnnotationInformationFactory> annotationInformationFactories() {
        return factories;
    }
}
