/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */


package org.wildfly.microprofile.reactive.messaging.config.amqp.ssl.context;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;
import org.wildfly.microprofile.reactive.messaging.common.security.ElytronSSLContextRegistry;

import javax.net.ssl.SSLContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AmqpReactiveMessagingCdiExtension implements Extension {

    private final Map<String, String> beansToAdd = Collections.synchronizedMap(new HashMap<>());

    public AmqpReactiveMessagingCdiExtension() {
    }

    public void registerAdditionalBeans(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        for (Map.Entry<String, String> beanEntry : beansToAdd.entrySet()) {
            afterBeanDiscovery.addBean()
                    .scope(Singleton.class)
                    .addQualifier(Identifier.Literal.of(beanEntry.getKey()))
                    .addQualifier(Any.Literal.INSTANCE)
                    .types(SSLContext.class)
                    .beanClass(SSLContext.class)
                    .createWith(
                            creationalContext ->
                                    ElytronSSLContextRegistry.getInstalledSSLContext(beanEntry.getValue()));

        }
    }

    public void addSslContextBean(String cdiBeanName, String sslContextName) {
        beansToAdd.put(cdiBeanName, sslContextName);
    }
}
