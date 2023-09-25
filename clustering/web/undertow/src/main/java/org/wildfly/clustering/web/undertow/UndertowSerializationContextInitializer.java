/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class UndertowSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public UndertowSerializationContextInitializer() {
        super(UndertowSerializationContextInitializerProvider.class);
    }
}
