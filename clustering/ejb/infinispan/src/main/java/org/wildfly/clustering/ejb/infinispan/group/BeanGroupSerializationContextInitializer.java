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

package org.wildfly.clustering.ejb.infinispan.group;

import java.util.Map;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.ejb.infinispan.EJBClientMarshallingProvider;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
public class BeanGroupSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalMarshaller<>(InfinispanBeanGroupKey.class, EJBClientMarshallingProvider.SESSION_ID, InfinispanBeanGroupKey::getId, InfinispanBeanGroupKey::new));
        context.registerMarshaller(new FunctionalMarshaller<>(InfinispanBeanGroupEntry.class, (Class<MarshalledValue<Map<Object, Object>, Object>>) (Class<?>) ByteBufferMarshalledValue.class, InfinispanBeanGroupEntry<Object, Object, Object>::getBeans, InfinispanBeanGroupEntry<Object, Object, Object>::new));
    }
}
