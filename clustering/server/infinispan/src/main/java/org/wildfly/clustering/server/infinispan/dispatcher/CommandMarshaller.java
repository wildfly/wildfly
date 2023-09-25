/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.dispatcher;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.wildfly.clustering.dispatcher.Command;

/**
 * Marshalling strategy for a command.
 * @author Paul Ferraro
 *
 * @param <C> command execution context
 */
public interface CommandMarshaller<C> {
    /**
     * Marshals the specified command to a byte[].
     * @param command a command
     * @return a serialized command.
     * @throws IOException if marshalling fails.
     */
    <R> ByteBuffer marshal(Command<R, ? super C> command) throws IOException;
}
