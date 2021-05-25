/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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