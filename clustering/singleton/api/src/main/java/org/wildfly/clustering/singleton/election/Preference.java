/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.election;

import org.wildfly.clustering.group.Node;

/**
 * An election preference.
 * @author Paul Ferraro
 * @deprecated To be removed without replacement.
 */
@Deprecated(forRemoval = true)
public interface Preference {
    boolean preferred(Node node);
}
