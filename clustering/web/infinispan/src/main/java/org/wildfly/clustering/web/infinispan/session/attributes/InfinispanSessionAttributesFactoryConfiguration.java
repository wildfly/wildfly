/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.attributes;

import java.util.function.Function;

import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.cache.session.attributes.fine.SessionAttributeActivationNotifier;

/**
 * @param <S> the HttpSession specification type
 * @param <C> the ServletContext specification type
 * @param <L> the HttpSessionActivationListener specification type
 * @param <V> attributes cache entry type
 * @param <SV> attributes serialized form type
 * @author Paul Ferraro
 */
public interface InfinispanSessionAttributesFactoryConfiguration<S, C, L, V, SV> extends InfinispanConfiguration, SessionAttributesFactoryConfiguration<S, C, L, V, SV> {

    Function<String, SessionAttributeActivationNotifier> getActivationNotifierFactory();
}
