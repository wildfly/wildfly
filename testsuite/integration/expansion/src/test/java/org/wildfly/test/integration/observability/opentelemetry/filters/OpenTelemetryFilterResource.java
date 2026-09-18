/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry.filters;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/")
public class OpenTelemetryFilterResource {
    @Inject
    private Meter meter;

    private LongCounter demo1;
    private LongCounter demo2;
    private LongCounter demo2dash1;
    private LongCounter demo3;
    private LongCounter demo4;
    private LongCounter demo5;
    private LongCounter taggedAlpha;
    private LongCounter taggedBravo;
    private LongCounter taggedCharlie;
    private LongCounter taggedDelta;
    private LongCounter negtestKeep;
    private LongCounter negtestDrop;

    @PostConstruct
    public void setupMeters() {
        demo1 = meter.counterBuilder("demo1").build();
        demo2 = meter.counterBuilder("demo2").build();
        demo2dash1 = meter.counterBuilder("demo2-1").build();
        demo3 = meter.counterBuilder("demo3").build();
        demo4 = meter.counterBuilder("demo4").build();
        demo5 = meter.counterBuilder("demo5").build();
        taggedAlpha = meter.counterBuilder("tagged.alpha").build();
        taggedBravo = meter.counterBuilder("tagged.bravo").build();
        taggedCharlie = meter.counterBuilder("tagged.charlie").build();
        taggedDelta = meter.counterBuilder("tagged.delta").build();
        negtestKeep = meter.counterBuilder("negtest.keep").build();
        negtestDrop = meter.counterBuilder("negtest.drop").build();
    }

    @GET
    @Path("/")
    public String sayHello() {
        demo1.add(1);
        demo2.add(1);
        demo2dash1.add(1);
        demo3.add(1);
        demo4.add(1);
        demo5.add(1);
        taggedAlpha.add(1, Attributes.of(AttributeKey.stringKey("env"), "prod"));
        taggedBravo.add(1, Attributes.of(AttributeKey.stringKey("env"), "staging"));
        taggedCharlie.add(1, Attributes.of(AttributeKey.stringKey("priority"), "high"));
        taggedDelta.add(1, Attributes.of(AttributeKey.stringKey("priority"), "low"));
        negtestKeep.add(1, Attributes.of(AttributeKey.stringKey("env"), "prod"));
        negtestDrop.add(1, Attributes.of(AttributeKey.stringKey("env"), "dev"));
        return "hello";
    }
}
