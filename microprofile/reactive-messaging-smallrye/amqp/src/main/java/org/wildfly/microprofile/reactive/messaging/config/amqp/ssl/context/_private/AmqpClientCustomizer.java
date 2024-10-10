/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.amqp.ssl.context._private;

import javax.net.ssl.SSLContext;

import jakarta.enterprise.context.ApplicationScoped;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.smallrye.reactive.messaging.ClientCustomizer;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.spi.tls.SslContextFactory;
import org.eclipse.microprofile.config.Config;
import org.wildfly.microprofile.reactive.messaging.common.security.ElytronSSLContextRegistry;

@ApplicationScoped
public class AmqpClientCustomizer implements ClientCustomizer<AmqpClientOptions> {
    @Override
    public AmqpClientOptions customize(String channel, Config channelConfig, AmqpClientOptions config) {

        String sslContextName =
                channelConfig.getOptionalValue(ElytronSSLContextRegistry.SSL_CONTEXT_PROPERTY, String.class).orElse(null);

        if (sslContextName != null) {
            final SSLContext context = ElytronSSLContextRegistry.getInstalledSSLContext(sslContextName);
            config.setSslEngineOptions(new JdkSSLEngineOptions() {
                        @Override
                        public SslContextFactory sslContextFactory() {
                            return new SslContextFactory() {
                                @Override
                                public SslContext create() {
                                    return new JdkSslContext(
                                            context,
                                            true,
                                            null,
                                            IdentityCipherSuiteFilter.INSTANCE,
                                            ApplicationProtocolConfig.DISABLED,
                                            io.netty.handler.ssl.ClientAuth.NONE,
                                            null,
                                            false);
                                }
                            };
                        }
                    });
        }

        return config;
    }
}
