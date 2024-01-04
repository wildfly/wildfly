/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.ejb;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.ejb.client.EJBClientSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;
import org.wildfly.clustering.weld.ejb.component.session.SessionComponentSerializationContextInitializer;
import org.wildfly.clustering.weld.ejb.component.stateful.StatefulComponentSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
public enum WeldEJBSerializationContextInitializerProvider implements SerializationContextInitializerProvider {

    CLIENT(new EJBClientSerializationContextInitializer()),
    WELD(new WeldModuleEJBSerializationContextInitializer()),
    WILDFLY(new WildFlyWeldEJBSerializationContextInitializer()),
    SESSION(new SessionComponentSerializationContextInitializer()),
    STATEFUL(new StatefulComponentSerializationContextInitializer()),
    ;
    private final SerializationContextInitializer initializer;

    WeldEJBSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
