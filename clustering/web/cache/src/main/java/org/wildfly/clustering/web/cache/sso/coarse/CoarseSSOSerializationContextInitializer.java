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

package org.wildfly.clustering.web.cache.sso.coarse;

import java.util.function.Function;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.AnyMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class CoarseSSOSerializationContextInitializer extends AbstractSerializationContextInitializer {

    private static final Function<SessionFilter<?, ?, Object>, Any> ACCESSOR = new Function<SessionFilter<?, ?, Object>, Any>() {
        @Override
        public Any apply(SessionFilter<?, ?, Object> filter) {
            return new Any(filter.getSession());
        }
    };
    private static final Function<Any, SessionFilter<?, ?, Object>> FACTORY = new Function<Any, SessionFilter<?, ?, Object>>() {
        @Override
        public SessionFilter<?, ?, Object> apply(Any any) {
            return new SessionFilter<>(any.get());
        }
    };

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalMarshaller<>((Class<SessionFilter<?, ?, Object>>) (Class<?>) SessionFilter.class, AnyMarshaller.INSTANCE, ACCESSOR, FACTORY));
    }
}
