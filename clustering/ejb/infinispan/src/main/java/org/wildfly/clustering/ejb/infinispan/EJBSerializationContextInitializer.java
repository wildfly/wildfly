/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;

/**
 * {@link SerializationContextInitializer} service for this module
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class EJBSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public EJBSerializationContextInitializer() {
        super(EJBSerializationContextInitializerProvider.class);
    }
}
