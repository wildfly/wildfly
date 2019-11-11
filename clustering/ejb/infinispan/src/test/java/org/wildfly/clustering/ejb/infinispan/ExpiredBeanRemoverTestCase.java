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

package org.wildfly.clustering.ejb.infinispan;

import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ejb.RemoveListener;

/**
 * Unit test for {@link ExpiredBeanRemover}.
 * @author Paul Ferraro
 */
public class ExpiredBeanRemoverTestCase {

    private final BeanFactory<String, Object> factory = mock(BeanFactory.class);
    private final ExpirationConfiguration<Object> expiration = mock(ExpirationConfiguration.class);
    private final BeanRemover<String, Object> remover = new ExpiredBeanRemover<>(this.factory, this.expiration);

    @Test
    public void locked() {
        RemoveListener<Object> listener = mock(RemoveListener.class);
        String id = "locked";

        when(this.factory.tryValue(id)).thenReturn(null);

        boolean result = this.remover.remove(id, listener);

        Assert.assertTrue(result);

        verify(this.factory, never()).remove(id, listener);
    }

    @Test
    public void notExpired() {
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        Duration timeout = Duration.ZERO;
        String id = "not-expired";

        when(this.factory.tryValue(id)).thenReturn(entry);
        when(this.expiration.getTimeout()).thenReturn(timeout);
        when(entry.isExpired(timeout)).thenReturn(false);

        boolean result = this.remover.remove(id, listener);

        Assert.assertFalse(result);

        verify(this.factory, never()).remove(id, listener);
    }

    @Test
    public void expired() {
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        Duration timeout = Duration.ZERO;
        String id = "expired";

        when(this.factory.tryValue(id)).thenReturn(entry);
        when(this.expiration.getTimeout()).thenReturn(timeout);
        when(entry.isExpired(timeout)).thenReturn(true);
        when(this.factory.remove(id, listener)).thenReturn(true);

        boolean result = this.remover.remove(id, listener);

        Assert.assertTrue(result);
    }

    @Test
    public void failedToExpired() {
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        Duration timeout = Duration.ZERO;
        String id = "not-expired";

        when(this.factory.tryValue(id)).thenReturn(entry);
        when(this.expiration.getTimeout()).thenReturn(timeout);
        when(entry.isExpired(timeout)).thenReturn(true);
        when(this.factory.remove(id, listener)).thenReturn(false);

        boolean result = this.remover.remove(id, listener);

        Assert.assertFalse(result);
    }
}
