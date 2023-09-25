/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.web;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class WeldWebSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public WeldWebSerializationContextInitializer() {
        super(WeldWebSerializationContextInitializerProvider.class);
    }
}
