/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;

/**
 * Configuration of a factory for creating a {@link SessionAttributes} object.
 * @param <S> the HttpSession specification type
 * @param <C> the ServletContext specification type
 * @param <L> the HttpSessionActivationListener specification type
 * @param <V> attributes cache entry type
 * @param <SV> attributes serialized form type
 * @author Paul Ferraro
 */
public interface SessionAttributesFactoryConfiguration<S, C, L, V, SV> {
    Marshaller<V, SV> getMarshaller();
    Immutability getImmutability();
    HttpSessionActivationListenerProvider<S, C, L> getHttpSessionActivationListenerProvider();
}
