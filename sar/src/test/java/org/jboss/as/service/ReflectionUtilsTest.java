/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ReflectionUtilsTest {

    public static class Foo {
        public int getA() {
            return 0;
        }

        public boolean isB() {
            return false;
        }

        public int getC(int c) {
            return c;
        }
    }

    @Test
    public void findNonBooleanGetter() throws Exception {
        final Method getter = ReflectionUtils.getGetter(Foo.class, "a");
        assertNotNull(getter);
        assertEquals("getA", getter.getName());
    }

    @Test
    public void findBooleanGetter() throws Exception {
        final Method getter = ReflectionUtils.getGetter(Foo.class, "b");
        assertNotNull(getter);
        assertEquals("isB", getter.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void doNotFindGetterWithArgument() throws Exception {
        ReflectionUtils.getGetter(Foo.class, "c");
        fail("Should have thrown exception - getC is not a getter");
    }
}
