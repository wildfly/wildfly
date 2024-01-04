/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.election;

import org.wildfly.clustering.group.Node;

public interface Preference {
    boolean preferred(Node node);
}
