/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.streams.operators;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveOperatorsSanityTest {
    @Test
    public void testReactiveApi() throws Exception {
        CompletionStage<List<String>> cs = ReactiveStreams.of("this", "is", "only", "a", "test")
                .map(s -> s.toUpperCase(Locale.ENGLISH)) // Transform the words
                .filter(s -> s.length() > 3) // Filter items
                .collect(Collectors.toList())
                .run();

        List<String> result = cs.toCompletableFuture().get();

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("THIS", result.get(0));
        Assert.assertEquals("ONLY", result.get(1));
        Assert.assertEquals("TEST", result.get(2));
    }
}
