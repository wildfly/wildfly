/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.test.shared.TimeoutUtil;
import org.junit.Assert;

/**
 * Add some utils to make porting of tests from Quarkus easier
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveMessagingTestUtils {

    public static void await(Supplier<Boolean> condition) {
        long end = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
        while (!condition.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (System.currentTimeMillis() > end) {
                throw new IllegalStateException("Timeout");
            }
        }
    }

    public static <T> void checkList(List<T> list, T... expected) {
        List<T> expectedList = Arrays.asList(expected);
        Assert.assertEquals(expectedList, list);
    }

}