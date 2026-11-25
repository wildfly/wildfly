/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.dispatcher;

import java.io.Serializable;

/**
 * A command to invoke remotely.
 *
 * @param <C> the command context type
 * @param <R> the command return type
 * @author Paul Ferraro
 * @deprecated Superseded by {@link org.wildfly.clustering.server.dispatcher.Command}.
 */
@Deprecated(forRemoval = true)
public interface Command<R, C> extends org.wildfly.clustering.server.dispatcher.Command<R, C, Exception>, Serializable {
}
