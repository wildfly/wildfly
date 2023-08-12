/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.coarse;

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
