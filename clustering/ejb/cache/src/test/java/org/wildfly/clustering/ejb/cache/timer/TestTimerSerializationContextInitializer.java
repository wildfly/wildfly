/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.util.List;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class TestTimerSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public TestTimerSerializationContextInitializer() {
        super(List.of(new TimerSerializationContextInitializer()));
    }
}
