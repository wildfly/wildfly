/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.ejb;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class WeldEJBSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public WeldEJBSerializationContextInitializer() {
        super(WeldEJBSerializationContextInitializerProvider.class);
    }
}
