package org.wildfly.test.preview.observability.micrometer;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * @author <a href="mailto:jasondlee@redhat.com">Jason Lee</a>
 */
@RequestScoped
@Path("/")
public class MetricResource {
    @Inject
    private MeterRegistry meterRegistry;
    private Counter counter;
    private Timer timer;

    @PostConstruct
    public void setupMeters() {
        counter = meterRegistry.counter("demo_counter");
        timer = meterRegistry.timer("demo_timer");
    }

    @GET
    @Path("/")
    public double getCount() {
        Timer.Sample sample = Timer.start();
        try {
            Thread.sleep((long) (Math.random() * 1000L));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        counter.increment();
        sample.stop(timer);

        return counter.count();
    }
}
