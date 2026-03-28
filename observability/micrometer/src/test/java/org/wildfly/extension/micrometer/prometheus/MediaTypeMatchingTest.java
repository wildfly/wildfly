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
                { new MediaType("text", "plain", 1), new MediaType("text", "plain", 1), true },
                // */plain -> invalid
                { new MediaType("*", "plain", 1), new MediaType("text", "*", 1), true },
                { new MediaType("*", "*", 1), new MediaType("application", "plain", 1), true },
                { new MediaType("text", "plain", 1), new MediaType("text", "*", 1), true },
                { new MediaType("text", "*", 1), new MediaType("text", "plain", 1), true },
                { new MediaType("text", "*", 1), new MediaType("application", "plain", 1), false },
                { new MediaType("text", "plain", 1), new MediaType("application", "vnd.google.protobuf", 0.8), false }
        });
    }

    @Test
    public void testMatching() {
        boolean matching = mediaType1.matches(mediaType2.type(), mediaType2.subtype());
        Assert.assertEquals(expectedMatch, matching);
    }

}
