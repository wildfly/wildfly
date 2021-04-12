/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.cache.function;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class FunctionSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalMarshaller<>(ConcurrentMapPutFunction.class, (Class<Map.Entry<Object, Object>>) (Class<?>) SimpleImmutableEntry.class, ConcurrentMapPutFunction<Object, Object>::getOperand, ConcurrentMapPutFunction<Object, Object>::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(ConcurrentMapRemoveFunction.class, Scalar.ANY, ConcurrentMapRemoveFunction::getOperand, ConcurrentMapRemoveFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(ConcurrentSetAddFunction.class, Scalar.ANY, ConcurrentSetAddFunction::getOperand, ConcurrentSetAddFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(ConcurrentSetRemoveFunction.class, Scalar.ANY, ConcurrentSetRemoveFunction::getOperand, ConcurrentSetRemoveFunction::new));
        context.registerMarshaller(new FunctionalMarshaller<>(CopyOnWriteMapPutFunction.class, (Class<Map.Entry<Object, Object>>) (Class<?>) SimpleImmutableEntry.class, CopyOnWriteMapPutFunction<Object, Object>::getOperand, CopyOnWriteMapPutFunction<Object, Object>::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(CopyOnWriteMapRemoveFunction.class, Scalar.ANY, CopyOnWriteMapRemoveFunction::getOperand, CopyOnWriteMapRemoveFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(CopyOnWriteSetAddFunction.class, Scalar.ANY, CopyOnWriteSetAddFunction::getOperand, CopyOnWriteSetAddFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(CopyOnWriteSetRemoveFunction.class, Scalar.ANY, CopyOnWriteSetRemoveFunction::getOperand, CopyOnWriteSetRemoveFunction::new));
    }
}
