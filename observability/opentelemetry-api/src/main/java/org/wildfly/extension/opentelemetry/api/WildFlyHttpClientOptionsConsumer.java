package org.wildfly.extension.opentelemetry.api;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.smallrye.opentelemetry.api.HttpClientOptionsConsumer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.spi.tls.SslContextFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WildFlyHttpClientOptionsConsumer implements HttpClientOptionsConsumer {
    private final WildFlyOpenTelemetryConfig config;

    public WildFlyHttpClientOptionsConsumer(WildFlyOpenTelemetryConfig config) {
        this.config = config;
    }

    @Override
    public void accept(HttpClientOptions httpClientOptions) {
        httpClientOptions.setSslEngineOptions(new JdkSSLEngineOptions() {
            @Override
            public SslContextFactory sslContextFactory() {
                return () -> new JdkSslContext(
                    config.getSslContext(),
                    true,
                    null,
                    IdentityCipherSuiteFilter.INSTANCE,
                    ApplicationProtocolConfig.DISABLED,
                    io.netty.handler.ssl.ClientAuth.NONE,
                    null,
                    false);
            }
        });
    }
}
