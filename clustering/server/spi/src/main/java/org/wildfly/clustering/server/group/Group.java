/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.group;

import org.wildfly.clustering.server.NodeFactory;

/**
 * {@link Group} that can create {@link org.wildfly.clustering.group.Node} instances.
 * @author Paul Ferraro
 * @param <A> address type
 */
public interface Group<A> extends org.wildfly.clustering.group.Group, NodeFactory<A> {
}
