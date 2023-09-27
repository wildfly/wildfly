/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.sso;

import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.web.LocalContextFactory;

/**
 * @author Paul Ferraro
 * @param <L> local context type
 */
public interface SSOManagerConfiguration<L> {
    Supplier<String> getIdentifierFactory();
    ByteBufferMarshaller getMarshaller();
    LocalContextFactory<L> getLocalContextFactory();
}
