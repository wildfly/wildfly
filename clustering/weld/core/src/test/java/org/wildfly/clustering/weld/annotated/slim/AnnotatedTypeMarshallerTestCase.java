/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;

import org.wildfly.clustering.marshalling.Tester;

/**
 * @author Paul Ferraro
 */
public abstract class AnnotatedTypeMarshallerTestCase {

    protected <X> void test(Tester<Annotated> tester, AnnotatedType<X> type) {
        tester.accept(type);

        for (AnnotatedConstructor<X> constructor : type.getConstructors()) {
            tester.accept(constructor);

            for (AnnotatedParameter<X> parameter : constructor.getParameters()) {
                tester.accept(parameter);
            }
        }

        for (AnnotatedField<? super X> field : type.getFields()) {
            tester.accept(field);
        }

        for (AnnotatedMethod<? super X> method : type.getMethods()) {
            tester.accept(method);

            for (AnnotatedParameter<? super X> parameter : method.getParameters()) {
                tester.accept(parameter);
            }
        }
    }
}
