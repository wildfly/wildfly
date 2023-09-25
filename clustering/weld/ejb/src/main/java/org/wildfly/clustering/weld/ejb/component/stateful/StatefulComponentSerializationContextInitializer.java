/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.ejb.component.stateful;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
public class StatefulComponentSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public StatefulComponentSerializationContextInitializer() {
        super("org.jboss.as.ejb3.component.stateful.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new StatefulSerializedProxyMarshaller());
    }
}
