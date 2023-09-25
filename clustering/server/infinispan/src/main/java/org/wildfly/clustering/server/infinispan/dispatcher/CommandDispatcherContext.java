/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.dispatcher;

import org.wildfly.clustering.context.Contextualizer;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;

/**
 * @author Paul Ferraro
 */
public interface CommandDispatcherContext<CC, MC> {
    CC getCommandContext();
    Contextualizer getContextualizer();
    MarshalledValueFactory<MC> getMarshalledValueFactory();
}
