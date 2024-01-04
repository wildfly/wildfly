/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class FacesSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public FacesSerializationContextInitializer() {
        super(FacesSerializationContextInitializerProvider.class);
    }
}
