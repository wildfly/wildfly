/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * Unit test for {@link ImmutableSessionAttributesFilter}.
 * @author Paul Ferraro
 */
public class ImmutableSessionAttributesFilterTestCase {

    @Test
    public void getListeners() {
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        SessionAttributesFilter filter = new ImmutableSessionAttributesFilter(attributes);

        Object object1 = new Object();
        Object object2 = new Object();
        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);

        when(attributes.getAttributeNames()).thenReturn(new HashSet<>(Arrays.asList("non-listener1", "non-listener2", "listener1", "listener2")));
        when(attributes.getAttribute("non-listener1")).thenReturn(object1);
        when(attributes.getAttribute("non-listener2")).thenReturn(object2);
        when(attributes.getAttribute("listener1")).thenReturn(listener1);
        when(attributes.getAttribute("listener2")).thenReturn(listener2);

        Map<String, Runnable> result = filter.getAttributes(Runnable.class);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.toString(), result.containsKey("listener1"));
        Assert.assertTrue(result.toString(), result.containsKey("listener2"));
        Assert.assertSame(listener1, result.get("listener1"));
        Assert.assertSame(listener2, result.get("listener2"));
    }
}
