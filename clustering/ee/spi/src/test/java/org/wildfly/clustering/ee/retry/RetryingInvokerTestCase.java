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

package org.wildfly.clustering.ee.retry;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.Test;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * @author Paul Ferraro
 */
public class RetryingInvokerTestCase {

    @Test
    public void testSupplier() throws Exception {
        Exception[] exceptions = new Exception[3];
        for (int i = 0; i < exceptions.length; ++i) {
            exceptions[i] = new Exception();
        }
        Object expected = new Object();
        ExceptionSupplier<Object, Exception> action = mock(ExceptionSupplier.class);

        Invoker invoker = new RetryingInvoker(Duration.ZERO, Duration.ofMillis(1));

        when(action.get()).thenReturn(expected);

        Object result = invoker.invoke(action);
        assertSame(expected, result);

        when(action.get()).thenThrow(exceptions[0]).thenReturn(expected);

        result = invoker.invoke(action);
        assertSame(expected, result);

        when(action.get()).thenThrow(exceptions[0], exceptions[1]).thenReturn(expected);

        result = invoker.invoke(action);
        assertSame(expected, result);

        when(action.get()).thenThrow(exceptions).thenReturn(expected);

        try {
            result = invoker.invoke(action);
            fail("Expected exception");
        } catch (Exception e) {
            assertSame(exceptions[2], e);
        }
    }

    @Test
    public void testRunnable() throws Exception {
        Exception[] exceptions = new Exception[3];
        for (int i = 0; i < exceptions.length; ++i) {
            exceptions[i] = new Exception();
        }
        ExceptionRunnable<Exception> action = mock(ExceptionRunnable.class);

        doNothing().when(action).run();

        Invoker invoker = new RetryingInvoker(Duration.ZERO, Duration.ZERO);

        invoker.invoke(action);

        doThrow(exceptions[0]).doNothing().when(action).run();

        invoker.invoke(action);

        doThrow(exceptions[0], exceptions[1]).doNothing().when(action).run();

        invoker.invoke(action);

        doThrow(exceptions).doNothing().when(action).run();

        try {
            invoker.invoke(action);
            fail("Expected exception");
        } catch (Exception e) {
            assertSame(exceptions[2], e);
        }
    }
}
