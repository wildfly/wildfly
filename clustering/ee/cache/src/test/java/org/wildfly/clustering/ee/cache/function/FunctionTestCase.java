/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class FunctionTestCase {
    @Test
    public void copyOnWriteSet() {
        Set<String> result = new CopyOnWriteSetAddFunction<>("foo").apply(null, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("foo"));

        Set<String> result2 = new CopyOnWriteSetAddFunction<>("bar").apply(null, result);
        Assert.assertNotNull(result2);
        Assert.assertNotSame(result, result2);
        Assert.assertTrue(result2.contains("foo"));
        Assert.assertTrue(result2.contains("bar"));

        Set<String> result3 = new CopyOnWriteSetRemoveFunction<>("foo").apply(null, result2);
        Assert.assertNotNull(result3);
        Assert.assertNotSame(result2, result3);
        Assert.assertFalse(result3.contains("foo"));
        Assert.assertTrue(result3.contains("bar"));

        Set<String> result4 = new CopyOnWriteSetRemoveFunction<>("bar").apply(null, result3);
        Assert.assertNull(result4);
    }

    @Test
    public void concurrentSet() {
        Set<String> result = new ConcurrentSetAddFunction<>("foo").apply(null, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("foo"));

        Set<String> result2 = new ConcurrentSetAddFunction<>("bar").apply(null, result);
        Assert.assertNotNull(result2);
        Assert.assertSame(result, result2);
        Assert.assertTrue(result2.contains("foo"));
        Assert.assertTrue(result2.contains("bar"));

        Set<String> result3 = new ConcurrentSetRemoveFunction<>("foo").apply(null, result2);
        Assert.assertNotNull(result3);
        Assert.assertSame(result2, result3);
        Assert.assertFalse(result3.contains("foo"));
        Assert.assertTrue(result3.contains("bar"));

        Set<String> result4 = new ConcurrentSetRemoveFunction<>("bar").apply(null, result3);
        Assert.assertNull(result4);
    }

    @Test
    public void copyOnWriteMap() {
        Map<String, String> result = new CopyOnWriteMapPutFunction<>("foo", "a").apply(null, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("foo"));

        Map<String, String> result2 = new CopyOnWriteMapPutFunction<>("bar", "b").apply(null, result);
        Assert.assertNotNull(result2);
        Assert.assertNotSame(result, result2);
        Assert.assertTrue(result2.containsKey("foo"));
        Assert.assertTrue(result2.containsKey("bar"));

        Map<String, String> result3 = new CopyOnWriteMapRemoveFunction<String, String>("foo").apply(null, result2);
        Assert.assertNotNull(result3);
        Assert.assertNotSame(result2, result3);
        Assert.assertFalse(result3.containsKey("foo"));
        Assert.assertTrue(result3.containsKey("bar"));

        Map<String, String> result4 = new CopyOnWriteMapRemoveFunction<String, String>("bar").apply(null, result3);
        Assert.assertNull(result4);
    }

    @Test
    public void concurrentMap() {
        Map<String, String> result = new ConcurrentMapPutFunction<>("foo", "a").apply(null, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("foo"));

        Map<String, String> result2 = new ConcurrentMapPutFunction<>("bar", "b").apply(null, result);
        Assert.assertNotNull(result2);
        Assert.assertSame(result, result2);
        Assert.assertTrue(result2.containsKey("foo"));
        Assert.assertTrue(result2.containsKey("bar"));

        Map<String, String> result3 = new ConcurrentMapRemoveFunction<String, String>("foo").apply(null, result2);
        Assert.assertNotNull(result3);
        Assert.assertSame(result2, result3);
        Assert.assertFalse(result3.containsKey("foo"));
        Assert.assertTrue(result3.containsKey("bar"));

        Map<String, String> result4 = new ConcurrentMapRemoveFunction<String, String>("bar").apply(null, result3);
        Assert.assertNull(result4);
    }
}
