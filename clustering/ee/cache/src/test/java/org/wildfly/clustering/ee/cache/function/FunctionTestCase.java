/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class FunctionTestCase {
    @Test
    public void set() {
        Set<String> result = new SetAddFunction<>("foo").apply(null, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("foo"));

        Set<String> result2 = new SetAddFunction<>("bar").apply(null, result);
        Assert.assertNotNull(result2);
        Assert.assertNotSame(result, result2);
        Assert.assertTrue(result2.contains("foo"));
        Assert.assertTrue(result2.contains("bar"));

        Set<String> result3 = new SetAddFunction<>(Set.of("baz", "qux")).apply(null, result2);
        Assert.assertNotNull(result3);
        Assert.assertNotSame(result2, result3);
        Assert.assertTrue(result3.contains("foo"));
        Assert.assertTrue(result3.contains("bar"));
        Assert.assertTrue(result3.contains("baz"));
        Assert.assertTrue(result3.contains("qux"));

        Set<String> result4 = new SetRemoveFunction<>("foo").apply(null, result3);
        Assert.assertNotNull(result4);
        Assert.assertNotSame(result3, result4);
        Assert.assertFalse(result4.contains("foo"));
        Assert.assertTrue(result4.contains("bar"));
        Assert.assertTrue(result4.contains("baz"));
        Assert.assertTrue(result4.contains("qux"));

        Set<String> result5 = new SetRemoveFunction<>(Set.of("bar", "baz")).apply(null, result4);
        Assert.assertNotNull(result5);
        Assert.assertNotSame(result4, result5);
        Assert.assertFalse(result5.contains("foo"));
        Assert.assertFalse(result5.contains("bar"));
        Assert.assertFalse(result5.contains("baz"));
        Assert.assertTrue(result5.contains("qux"));

        Set<String> result6 = new SetRemoveFunction<>("qux").apply(null, result5);
        Assert.assertNull(result6);
    }

    @Test
    public void map() {
        Map<String, String> result = new MapPutFunction<>("foo", "a").apply(null, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("foo"));

        Map<String, String> result2 = new MapPutFunction<>("bar", "b").apply(null, result);
        Assert.assertNotNull(result2);
        Assert.assertNotSame(result, result2);
        Assert.assertTrue(result2.containsKey("foo"));
        Assert.assertTrue(result2.containsKey("bar"));

        Map<String, String> result3 = new MapRemoveFunction<String, String>("foo").apply(null, result2);
        Assert.assertNotNull(result3);
        Assert.assertNotSame(result2, result3);
        Assert.assertFalse(result3.containsKey("foo"));
        Assert.assertTrue(result3.containsKey("bar"));

        Map<String, String> result4 = new MapRemoveFunction<String, String>("bar").apply(null, result3);
        Assert.assertNull(result4);

        Map<String, String> result5 = new MapComputeFunction<>(Map.of("foo", "a", "bar", "b")).apply(null, result4);
        Assert.assertNotNull(result5);
        Assert.assertEquals(2, result5.size());
        Assert.assertEquals("a", result5.get("foo"));
        Assert.assertEquals("b", result5.get("bar"));

        Map<String, String> updates = new TreeMap<>();
        updates.put("foo", null);
        updates.put("bar", "c");
        Map<String, String> result6 = new MapComputeFunction<>(updates).apply(null, result5);
        Assert.assertNotNull(result6);
        Assert.assertEquals(1, result6.size());
        Assert.assertFalse(result6.containsKey("foo"));
        Assert.assertEquals("c", result6.get("bar"));

        Map<String, String> result7 = new MapComputeFunction<>(Collections.<String, String>singletonMap("bar", null)).apply(null, result6);
        Assert.assertNull(result7);
    }
}
