/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.util;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class UtilSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public UtilSerializationContextInitializer() {
        super("com.sun.faces.util.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new LRUMapMarshaller());
    }
}
