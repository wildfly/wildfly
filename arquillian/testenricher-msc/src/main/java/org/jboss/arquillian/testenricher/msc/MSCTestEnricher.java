/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.arquillian.testenricher.msc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;

import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;

/**
 * The MSC TestEnricher
 *
 * The enricher supports the injection of the {@link ServiceTarget} that
 * is associated with this test case.
 *
 * <pre>
 * <code>
 *    @Inject
 *    ServiceTarget serviceTarget;
 *
 *    @Inject
 *    ServiceContainer serviceContainer;
 * </code>
 * </pre>
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Nov-2010
 */
public class MSCTestEnricher implements TestEnricher {

    @Override
    public void enrich(Object testCase) {
        Class<? extends Object> testClass = testCase.getClass();
        for (Field field : testClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                if (field.getType().isAssignableFrom(ServiceContainer.class)) {
                    injectServiceContainer(testCase, field);
                }
                else if (field.getType().isAssignableFrom(ServiceTarget.class)) {
                    injectServiceTarget(testCase, field);
                }
            }
        }
    }

    @Override
    public Object[] resolve(Method method) {
        return null;
    }

    private void injectServiceContainer(Object testCase, Field field) {
        try {
            ServiceContainer serviceContainer = CurrentServiceContainer.getServiceContainer();
            field.set(testCase, serviceContainer);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot inject ServiceContainer", ex);
        }
    }

    private void injectServiceTarget(Object testCase, Field field) {
        try {
            ServiceTarget serviceTarget = ServiceTargetAssociation.getServiceTarget(testCase.getClass().getName());
            field.set(testCase, serviceTarget);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot inject ServiceTarget", ex);
        }
    }
}
