package org.wildfly.test.integration.microprofile.metrics.metadata.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Counter;

@ApplicationScoped
public class CustomCounterMetric implements Counter {

    /**
     * For the test to pass, the multiplier value must be configured wit 2
     * as specified in the microprofile-config.properties bundled in the deployment.
     */
    @Inject
    @ConfigProperty(defaultValue = "0")
    int multiplier;

    private int count = 0;

    @Override
    public void inc() {
        inc(1);
    }

    @Override
    public void inc(long n) {
        count += multiplier * n;
    }

    @Override
    public long getCount() {
        return count;
    }
}
