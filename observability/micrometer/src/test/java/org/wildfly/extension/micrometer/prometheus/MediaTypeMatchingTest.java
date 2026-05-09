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
public class MediaTypeMatchingTest {

    private final MediaType mediaType1;
    private final MediaType mediaType2;
    private final boolean expectedMatch;

    public MediaTypeMatchingTest(MediaType mediaType1, MediaType mediaType2, boolean expectedMatch) {
        this.mediaType1 = mediaType1;
        this.mediaType2 = mediaType2;
        this.expectedMatch = expectedMatch;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { new MediaType("text", "plain", Map.of()), new MediaType("text", "plain", Map.of("q", "1.0")), true },
                // */plain -> invalid
                { new MediaType("*", "plain", Map.of()), new MediaType("text", "*", Map.of("q", "1.0")), true },
                { new MediaType("*", "*", Map.of()), new MediaType("application", "plain", Map.of("q", "1.0")), true },
                { new MediaType("text", "plain", Map.of()), new MediaType("text", "*", Map.of("q", "1.0")), true },
                { new MediaType("text", "*", Map.of()), new MediaType("text", "plain", Map.of("q", "1.0")), true },
                { new MediaType("text", "*", Map.of()), new MediaType("application", "plain", Map.of("q", "1.0")), false },
                { new MediaType("text", "plain", Map.of()), new MediaType("application", "vnd.google.protobuf", Map.of("q", "0.8")), false }
        });
    }

    @Test
    public void testMatching() {
        boolean matching = mediaType1.matches(mediaType2.type(), mediaType2.subtype());
        Assert.assertEquals(expectedMatch, matching);
    }

}
