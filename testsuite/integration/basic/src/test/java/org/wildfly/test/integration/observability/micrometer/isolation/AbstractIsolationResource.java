/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.isolation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

public abstract class AbstractIsolationResource {

    public static final String TAG_WF_DEPLOYMENT = "WF_DEPLOYMENT";
    private final String deploymentName;
    protected final String tag;
    protected final String otherTag;

    @Inject
    protected MeterRegistry meterRegistry;
    protected Counter counter;

    public AbstractIsolationResource(String deploymentName, String tag, String otherTag) {
        this.deploymentName = deploymentName + ".war";
        this.tag = tag;
        this.otherTag = otherTag;
    }

    @PostConstruct
    public void setupMeters() {
        final Tags tags = Tags.of("app", tag);
        counter = meterRegistry.counter(tag + "_counter", tags);
        meterRegistry.timer(tag + "_timer", tags);
        meterRegistry.gauge(tag + "_gauge", tags, new ArrayList<String>(), List::size);
        meterRegistry.summary(tag + "_summary", tags);
    }

    @GET
    public String ping() {
        counter.increment();
        return "ping";
    }

    @GET
    @Path("counter")
    public Response counter() {
        checkMeterVisibility(() -> meterRegistry.counter(otherTag + "_counter", "app", otherTag),
                RequiredSearch::counter);
        return Response.ok().build();
    }

    @GET
    @Path("timer")
    public Response timer() {
        checkMeterVisibility(() -> meterRegistry.timer(otherTag + "_timer", "app", otherTag),
                RequiredSearch::timer);
        return Response.ok().build();
    }

    @GET
    @Path("gauge")
    public Response gauge() {
        checkMeterVisibility(() -> {
                    // MeterRegistry.gauge doesn't return the Meter like other methods do, so we have to .get it separately
                    meterRegistry.gauge(otherTag + "_gauge", Tags.of("app", otherTag), new ArrayList<>(), List::size);
                    return meterRegistry.get(otherTag + "_gauge").tags(Tags.of("app", otherTag)).gauge();
                },
                RequiredSearch::gauge);
        return Response.ok().build();
    }

    @GET
    @Path("summary")
    public Response summary() {
        checkMeterVisibility(() -> meterRegistry.summary(otherTag + "_summary", "app", otherTag),
                RequiredSearch::summary);
        return Response.ok().build();
    }

    @GET
    @Path("find")
    public Response find() {
        try {
            var meter = meterRegistry.find(otherTag + "_counter")
                    .tag("app", otherTag)
                    .counter();
            if (containsWrongDeploymentTag(meter.getId())) {
                throw new InternalServerErrorException("Other meter should not be found.");
            }
        } catch (MeterNotFoundException e) {
            //
        }
        return Response.ok().build();
    }

    @GET
    @Path("getMeters")
    public Response getMeters() {
        List<Meter> meters = meterRegistry.getMeters();
        if (meters.stream().anyMatch(m -> {
            var tag = m.getId().getTag(TAG_WF_DEPLOYMENT);
            return tag != null && !tag.equals(deploymentName);
        })) {
            return Response.serverError().build();
        }

        return Response.ok().build();

    }

    @GET
    @Path("forEachMeter")
    public Response forEachMeter() {
        meterRegistry.forEachMeter(m -> {
            var tag = m.getId().getTag(TAG_WF_DEPLOYMENT);
           if (tag != null && !tag.equals(deploymentName)) {
                throw new InternalServerErrorException();
            }
        });

        return Response.ok().build();
    }

    private boolean containsWrongDeploymentTag(Meter.Id id) {
        // If the tag doesn't match the current deployment, whether it's missing or different, it should be denied
        return !deploymentName.equals(id.getTag(TAG_WF_DEPLOYMENT));
    }

    private <T extends Meter> void checkMeterVisibility(Supplier<T> producer, Function<RequiredSearch, T> searchHandler) {
        T meter = producer.get();

        if (containsWrongDeploymentTag(meter.getId())) {
            throw new InternalServerErrorException("'Other' meter marker tag should match. Meters are leaking between deployments.");
        }

        try {
            T found = searchHandler.apply(meterRegistry.get(meter.getId().getName()).tags("app", otherTag));
            if (containsWrongDeploymentTag(found.getId())) {
                throw new InternalServerErrorException("Other meter should not be found.");
            }
        } catch (MeterNotFoundException e) {
            //
        }
    }
}
