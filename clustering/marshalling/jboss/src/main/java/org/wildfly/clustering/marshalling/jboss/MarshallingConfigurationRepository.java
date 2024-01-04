/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.marshalling.jboss;

import org.jboss.marshalling.MarshallingConfiguration;

/**
 * Repository of versioned {@link MarshallingConfiguration}s.
 * @author Paul Ferraro
 */
public interface MarshallingConfigurationRepository {
    int getCurrentMarshallingVersion();

    MarshallingConfiguration getMarshallingConfiguration(int version);
}