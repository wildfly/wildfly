/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
