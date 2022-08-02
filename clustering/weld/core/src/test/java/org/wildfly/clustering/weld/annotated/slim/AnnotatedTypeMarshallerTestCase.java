/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
