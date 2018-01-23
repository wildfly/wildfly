/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan.bean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.clustering.ejb.infinispan.BeanEntry;
import org.wildfly.clustering.ejb.infinispan.BeanGroup;
import org.wildfly.clustering.ejb.infinispan.BeanRemover;

public class InfinispanBeanTestCase {

    private final String id = "id";
    private final BeanEntry<String> entry = mock(BeanEntry.class);
    private final BeanGroup<String, Object> group = mock(BeanGroup.class);
    private final Mutator mutator = mock(Mutator.class);
    private final BeanRemover<String, Object> remover = mock(BeanRemover.class);
    private final Time timeout = new Time(1, TimeUnit.MINUTES);
    private final PassivationListener<Object> listener = mock(PassivationListener.class);

    private final Bean<String, Object> bean = new InfinispanBean<>(this.id, this.entry, this.group, this.mutator, this.remover, this.timeout, this.listener);

    @After
    public void tearDown() {
        reset(this.entry, this.group, this.remover, this.listener);
    }

    @Test
    public void getId() {
        Assert.assertSame(this.id, this.bean.getId());
    }

    @Test
    public void getGroupId() {
        String groupId = "group";
        when(this.entry.getGroupId()).thenReturn(groupId);
        String result = this.bean.getGroupId();
        Assert.assertSame(groupId, result);
    }

    @Test
    public void acquire() {
        Object value = new Object();
        when(this.group.getBean(this.id, this.listener)).thenReturn(value);
        Object result = this.bean.acquire();
        Assert.assertSame(value, result);
    }

    @Test
    public void release() {
        this.bean.release();

        verify(this.group).releaseBean(this.id, this.listener);
    }

    @Test
    public void isExpired() {
        when(this.entry.getLastAccessedTime()).thenReturn(null);
        Assert.assertFalse(this.bean.isExpired());

        long now = System.currentTimeMillis();
        when(this.entry.getLastAccessedTime()).thenReturn(new Date(now));
        Assert.assertFalse(this.bean.isExpired());

        when(this.entry.getLastAccessedTime()).thenReturn(new Date(now - this.timeout.convert(TimeUnit.MILLISECONDS) - 1));
        Assert.assertTrue(this.bean.isExpired());
    }

    @Test
    public void remove() {
        RemoveListener<Object> listener = mock(RemoveListener.class);

        when(this.group.isCloseable()).thenReturn(false);

        this.bean.remove(listener);

        verify(this.remover).remove(this.id, listener);

        this.bean.remove(listener);

        verifyNoMoreInteractions(this.remover);
    }

    @Test
    public void close() {
        when(this.entry.getLastAccessedTime()).thenReturn(null);
        when(this.group.isCloseable()).thenReturn(false);

        this.bean.close();

        verify(this.entry).setLastAccessedTime(ArgumentMatchers.<Date>any());
        verify(this.mutator, never()).mutate();
        verify(this.group, never()).close();

        reset(this.entry, this.mutator, this.group);

        when(this.entry.getLastAccessedTime()).thenReturn(new Date());
        when(this.group.isCloseable()).thenReturn(true);

        this.bean.close();

        verify(this.entry).setLastAccessedTime(ArgumentMatchers.<Date>any());
        verify(this.mutator).mutate();
        verify(this.group).close();
    }
}
