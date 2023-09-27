/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server;

import org.wildfly.clustering.group.Node;

/**
 * @author Paul Ferraro
 */
public interface NodeFactory<A> {

    Node createNode(A address);
}
