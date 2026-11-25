/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class MediaTypeParsingTest {

    private final String mediaTypeString;
    private final MediaType expectedMediaType;

    public MediaTypeParsingTest(String mediaTypeString, MediaType expectedMediaType) {
        this.mediaTypeString = mediaTypeString;
        this.expectedMediaType = expectedMediaType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { "", new MediaType("*", "*", Map.of("q", "1.0")) },
                { "text/plain; version=0.0.4; charset=utf-8",
                        new MediaType("text", "plain", Map.of("version", "0.0.4", "charset", "utf-8", "q", "1.0")) },
                { "application/openmetrics-text; version=1.0.0; charset=utf-8",
                        new MediaType("application", "openmetrics-text", Map.of("version", "1.0.0", "charset", "utf-8", "q", "1.0")) },
                { "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily; encoding=delimited",
                        new MediaType("application", "vnd.google.protobuf", Map.of("proto", "io.prometheus.client.MetricFamily", "encoding", "delimited", "q", "1.0")) },
                { "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily; encoding=delimited; q=0.8",
                        new MediaType("application", "vnd.google.protobuf", Map.of("proto", "io.prometheus.client.MetricFamily", "encoding", "delimited", "q", "0.8")) } });
    }

    @Test
    public void testParsing() {
        MediaType parsedMediaType = MediaType.parse(mediaTypeString);
        Assert.assertEquals(expectedMediaType, parsedMediaType);
    }

}
