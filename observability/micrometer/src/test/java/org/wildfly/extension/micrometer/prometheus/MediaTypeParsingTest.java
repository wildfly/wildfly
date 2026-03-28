/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

import java.util.Arrays;
import java.util.Collection;

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
        return Arrays.asList(new Object[][] { { "", new MediaType("*", "*", 1) },
                { "text/plain; version=0.0.4; charset=utf-8", new MediaType("text", "plain", 1) },
                { "application/openmetrics-text; version=1.0.0; charset=utf-8",
                        new MediaType("application", "openmetrics-text", 1) },
                { "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily; encoding=delimited",
                        new MediaType("application", "vnd.google.protobuf", 1) },
                { "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily; encoding=delimited; q=0.8",
                        new MediaType("application", "vnd.google.protobuf", 0.8) } });
    }

    @Test
    public void testParse() {
        MediaType parsedMediaType = MediaType.parse(mediaTypeString);
        Assert.assertEquals(expectedMediaType, parsedMediaType);
    }

}
