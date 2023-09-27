/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim;

import java.io.IOException;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;

import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public abstract class AnnotatedTypeMarshallerTestCase {

    private final Tester<Annotated> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

    protected <X> void test(AnnotatedType<X> type) throws IOException {
        this.tester.test(type);

        for (AnnotatedConstructor<X> constructor : type.getConstructors()) {
            this.tester.test(constructor);

            for (AnnotatedParameter<X> parameter : constructor.getParameters()) {
                this.tester.test(parameter);
            }
        }

        for (AnnotatedField<? super X> field : type.getFields()) {
            this.tester.test(field);
        }

        for (AnnotatedMethod<? super X> method : type.getMethods()) {
            this.tester.test(method);

            for (AnnotatedParameter<? super X> parameter : method.getParameters()) {
                this.tester.test(parameter);
            }
        }
    }
}
