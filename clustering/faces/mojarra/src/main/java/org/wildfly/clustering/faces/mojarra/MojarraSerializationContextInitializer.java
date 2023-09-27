/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.faces.FacesSerializationContextInitializerProvider;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class MojarraSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public MojarraSerializationContextInitializer() {
        super(new CompositeSerializationContextInitializer(FacesSerializationContextInitializerProvider.class), new CompositeSerializationContextInitializer(MojarraSerializationContextInitializerProvider.class));
    }
}
