/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.dispatcher;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * Configuration for a {@link ChannelCommandDispatcherFactory}.
 * @author Paul Ferraro
 */
public interface ChannelCommandDispatcherFactoryConfiguration {
    Predicate<Message> getUnknownForkPredicate();
    JChannel getChannel();
    ByteBufferMarshaller getMarshaller();
    Duration getTimeout();
    Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory();
}
