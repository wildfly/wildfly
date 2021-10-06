package org.wildfly.test.manual.observability.opentelemetry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

public class JaegerContainer extends GenericContainer<JaegerContainer> {
    private static final String JAEGER_IMAGE = "jaegertracing/all-in-one:latest";

    public JaegerContainer() {
        super(JAEGER_IMAGE);
    }

    @Override
    protected void configure() {
        final List<Integer> udp = Arrays.asList(5775, 6831, 6832);
        final List<Integer> tcp = Arrays.asList(5778, 9411, 14250, 14268, 16686);

        udp.forEach(p -> addFixedExposedPort(p, p, InternetProtocol.UDP));
        tcp.forEach(p -> addFixedExposedPort(p, p));

        withEnv("COLLECTOR_ZIPKIN_HOST_PORT", "9411");
        waitingFor(new HostPortWaitStrategy() {
            @Override
            protected Set<Integer> getLivenessCheckPorts() {
                return new HashSet<>(tcp);
            }
        });
    }
}
