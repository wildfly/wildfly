/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueMarshaller;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;

/**
 * Configuration for a factory for creating {@link SessionAttributes} objects, based on marshalled values.
 * @author Paul Ferraro
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <V> the attributes value type
 * @param <LC> the local context type
 */
public abstract class MarshalledValueSessionAttributesFactoryConfiguration<S, SC, AL, V, LC> implements SessionAttributesFactoryConfiguration<S, SC, AL, V, MarshalledValue<V, ByteBufferMarshaller>> {
    private final Immutability immutability;
    private final Marshaller<V, MarshalledValue<V, ByteBufferMarshaller>> marshaller;
    private final HttpSessionActivationListenerProvider<S, SC, AL> provider;

    protected MarshalledValueSessionAttributesFactoryConfiguration(SessionManagerFactoryConfiguration<S, SC, AL, LC> configuration) {
        this.immutability = configuration.getImmutability();
        this.marshaller = new MarshalledValueMarshaller<>(new ByteBufferMarshalledValueFactory(configuration.getMarshaller()));
        this.provider = configuration.getSpecificationProvider();
    }

    @Override
    public Marshaller<V, MarshalledValue<V, ByteBufferMarshaller>> getMarshaller() {
        return this.marshaller;
    }

    @Override
    public Immutability getImmutability() {
        return this.immutability;
    }

    @Override
    public HttpSessionActivationListenerProvider<S, SC, AL> getHttpSessionActivationListenerProvider() {
        return this.provider;
    }
}