/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.context;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ThreadFactory;

import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ContextualThreadFactoryTestCase {

    @Test
    public void test() {
        ThreadFactory factory = mock(ThreadFactory.class);
        Object targetContext = new Object();
        ThreadContextReference<Object> reference = mock(ThreadContextReference.class);
        Contextualizer contextualizer = mock(Contextualizer.class);
        ThreadFactory subject = new ContextualThreadFactory<>(factory, targetContext, reference, contextualizer);
        Runnable task = mock(Runnable.class);
        Runnable contextualTask = mock(Runnable.class);
        Thread expected = new Thread();

        when(contextualizer.contextualize(task)).thenReturn(contextualTask);
        when(factory.newThread(same(contextualTask))).thenReturn(expected);

        Thread result = subject.newThread(task);

        assertSame(expected, result);

        verify(reference).accept(same(expected), same(targetContext));
    }
}
