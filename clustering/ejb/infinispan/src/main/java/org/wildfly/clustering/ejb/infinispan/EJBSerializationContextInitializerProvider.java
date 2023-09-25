/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.ee.cache.function.FunctionSerializationContextInitializer;
import org.wildfly.clustering.ee.cache.offset.OffsetSerializationContextInitializer;
import org.wildfly.clustering.ejb.cache.bean.BeanSerializationContextInitializer;
import org.wildfly.clustering.ejb.cache.timer.TimerSerializationContextInitializer;
import org.wildfly.clustering.ejb.client.EJBClientSerializationContextInitializer;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanSerializationContextInitializer;
import org.wildfly.clustering.ejb.infinispan.network.NetworkEJBSerializationContextInitializer;
import org.wildfly.clustering.ejb.infinispan.network.NetworkMarshallingProvider;
import org.wildfly.clustering.ejb.infinispan.timer.InfinispanTimerSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;

/**
 * {@link SerializationContextInitializer} provider for this module.
 * @author Paul Ferraro
 */
public enum EJBSerializationContextInitializerProvider implements SerializationContextInitializerProvider {

    NETWORK(new ProviderSerializationContextInitializer<>("org.jboss.as.network.proto", NetworkMarshallingProvider.class)),
    INFINISPAN_NETWORK(new NetworkEJBSerializationContextInitializer()),
    EJB_CLIENT(new EJBClientSerializationContextInitializer()),
    OFFSET(new OffsetSerializationContextInitializer()),
    FUNCTION(new FunctionSerializationContextInitializer()),
    BEAN(new BeanSerializationContextInitializer()),
    INFINISPAN_BEAN(new InfinispanBeanSerializationContextInitializer()),
    TIMER(new TimerSerializationContextInitializer()),
    INFINISPAN_TIMER(new InfinispanTimerSerializationContextInitializer()),
    ;

    private final SerializationContextInitializer initializer;

    EJBSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
