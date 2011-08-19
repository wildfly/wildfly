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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.as.ee.metadata.AbstractEEAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.annotation.ScheduleAnnotationInformationFactory;
import org.jboss.as.ejb3.deployment.processors.annotation.TimeoutAnnotationInformationFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Processes the @Timeout annotation on an EJB, and adds the configurator that is responsible for loading the method from the reflection index.
 * <p/>
 * The processor also handles auto timers (the @Schedule annotation)
 *
 * @author Stuart Douglas
 */
public class TimeoutAnnotationProcessor extends AbstractEEAnnotationProcessor {

    final List<ClassAnnotationInformationFactory> factories;

    public TimeoutAnnotationProcessor() {
        List<ClassAnnotationInformationFactory> factories = new ArrayList<ClassAnnotationInformationFactory>();
        factories.add(new TimeoutAnnotationInformationFactory());
        factories.add(new ScheduleAnnotationInformationFactory());
        this.factories = Collections.unmodifiableList(factories);
    }


    @Override
    protected List<ClassAnnotationInformationFactory> annotationInformationFactories() {
        return factories;
    }
}
