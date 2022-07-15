package org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.util;

import org.jboss.arquillian.container.spi.event.StartClassContainers;
import org.jboss.arquillian.container.spi.event.StopClassContainers;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.concurrent.atomic.AtomicReference;

public class ElasticsearchServerSetupObserver {
    private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:7.16.3";

    private static final AtomicReference<String> httpHostAddress = new AtomicReference<>();

    public static String getHttpHostAddress() {
        String address = httpHostAddress.get();
        if (address == null) {
            throw new IllegalStateException(ElasticsearchServerSetupObserver.class + " wasn't notified of the StartClassContainers event");
        }
        return address;
    }

    private final Object elasticsearchContainer;

    public ElasticsearchServerSetupObserver() {

        // This observer can be invoked by Arquillian in environments where Docker is not available,
        // even though the test itself is disabled, e.g. with an org.junit.Assume in a @BeforeClass method.
        // Hence this hack: if Docker is not available,
        // we simply don't create a container and the observer acts as a no-op.
        if (!AssumeTestGroupUtil.isDockerAvailable()) {
            this.elasticsearchContainer = null;
            return;
        }

        try {
            // Unfortunately the observer is automatically installed on the server side too,
            // where it simply cannot work due to testcontainers not being available.
            // Hence this hack: if testcontainers is not available,
            // we simply don't create a container and the observer acts as a no-op.
            Class.forName( "org.testcontainers.elasticsearch.ElasticsearchContainer" );
        } catch (ClassNotFoundException e) {
            this.elasticsearchContainer = null;
            return;
        }
        this.elasticsearchContainer = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
                // Limit the RAM usage.
                // Recent versions of ES limit themselves to 50% of the total available RAM,
                // but on CI this can be too much, as we also have the Maven JVM
                // and the JVMs that runs tests taking up a significant amount of RAM.
                .withEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g");
    }

    public void startElasticsearch(@Observes StartClassContainers event) {
        ElasticsearchContainer theContainer = (ElasticsearchContainer) elasticsearchContainer;
        if (theContainer != null) {
            theContainer.start();
            if (!httpHostAddress.compareAndSet(null, theContainer.getHttpHostAddress())) {
                throw new IllegalStateException("Cannot run two Elasticsearch-based tests in parallel");
            }
        }
    }

    public void stopElasticsearch(@Observes StopClassContainers event) {
        httpHostAddress.set(null);
        ElasticsearchContainer theContainer = (ElasticsearchContainer) elasticsearchContainer;
        if (theContainer != null && theContainer.isRunning()) {
            theContainer.stop();
        }
    }
}
