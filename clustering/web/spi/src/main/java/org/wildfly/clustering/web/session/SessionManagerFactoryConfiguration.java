/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.session;

import java.util.function.Supplier;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.web.WebDeploymentConfiguration;

/**
 * Encapsulates the configuration of a session manager.
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <MC> the marshalling context type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public interface SessionManagerFactoryConfiguration<S, SC, AL, LC> extends WebDeploymentConfiguration {

    Integer getMaxActiveSessions();

    ByteBufferMarshaller getMarshaller();

    Supplier<LC> getLocalContextFactory();

    Immutability getImmutability();

    SpecificationProvider<S, SC, AL> getSpecificationProvider();

    SessionAttributePersistenceStrategy getAttributePersistenceStrategy();
}
