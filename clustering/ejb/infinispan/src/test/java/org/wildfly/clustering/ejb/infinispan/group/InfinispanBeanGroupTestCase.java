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
package org.wildfly.clustering.ejb.infinispan.group;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.infinispan.BeanGroup;
import org.wildfly.clustering.ejb.infinispan.BeanGroupEntry;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

public class InfinispanBeanGroupTestCase {
    private String id;
    private BeanGroupEntry<String, Object> entry = mock(BeanGroupEntry.class);
    private MarshallingContext context = mock(MarshallingContext.class);
    private Mutator mutator = mock(Mutator.class);
    private Remover<String> remover = mock(Remover.class);

    private BeanGroup<String, Object> group = new InfinispanBeanGroup<>(this.id, this.entry, this.context, this.mutator, this.remover);

    @Test
    public void getId() {
        Assert.assertSame(this.id, this.group.getId());
    }

    @Test
    public void isCloseable() {
        when(this.entry.totalUsage()).thenReturn(1);

        Assert.assertFalse(this.group.isCloseable());

        when(this.entry.totalUsage()).thenReturn(0);

        Assert.assertTrue(this.group.isCloseable());
    }

    @Test
    public void getBeans() throws ClassNotFoundException, IOException {
        MarshalledValue<Map<String, Object>, MarshallingContext> value = mock(MarshalledValue.class);
        Map<String, Object> beans = Collections.singletonMap("id", new Object());

        when(this.entry.getBeans()).thenReturn(value);
        when(value.get(this.context)).thenReturn(beans);

        Assert.assertSame(beans.keySet(), this.group.getBeans());
    }

    @Test
    public void addBean() throws ClassNotFoundException, IOException {
        MarshalledValue<Map<String, Object>, MarshallingContext> value = mock(MarshalledValue.class);
        Map<String, Object> beans = mock(Map.class);
        String id = "id";
        Object bean = new Object();

        when(this.entry.getBeans()).thenReturn(value);
        when(value.get(this.context)).thenReturn(beans);

        this.group.addBean(id, bean);

        verify(beans).put(id, bean);
    }

    @Test
    public void getBean() throws ClassNotFoundException, IOException {
        PassivationListener<Object> listener = mock(PassivationListener.class);
        MarshalledValue<Map<String, Object>, MarshallingContext> value = mock(MarshalledValue.class);
        Map<String, Object> beans = mock(Map.class);
        String id = "id";
        Object bean = new Object();

        when(this.entry.getBeans()).thenReturn(value);
        when(value.get(this.context)).thenReturn(beans);
        when(beans.get(id)).thenReturn(bean);
        when(this.entry.incrementUsage(id)).thenReturn(1);

        Object result = this.group.getBean(id, listener);

        Assert.assertSame(bean, result);

        verifyZeroInteractions(listener);

        when(this.entry.incrementUsage(id)).thenReturn(0);

        result = this.group.getBean(id, listener);

        Assert.assertSame(bean, result);

        verify(listener).postActivate(bean);
    }

    @Test
    public void releaseBean() throws ClassNotFoundException, IOException {
        PassivationListener<Object> listener = mock(PassivationListener.class);
        MarshalledValue<Map<String, Object>, MarshallingContext> value = mock(MarshalledValue.class);
        Map<String, Object> beans = mock(Map.class);
        String id = "id";
        Object bean = new Object();

        when(this.entry.decrementUsage(id)).thenReturn(1);

        boolean result = this.group.releaseBean(id, listener);

        Assert.assertFalse(result);

        verifyZeroInteractions(listener);
        verify(this.entry, never()).getBeans();

        when(this.entry.decrementUsage(id)).thenReturn(0);
        when(this.entry.getBeans()).thenReturn(value);
        when(value.get(this.context)).thenReturn(beans);
        when(beans.get(id)).thenReturn(bean);

        result = this.group.releaseBean(id, listener);

        Assert.assertTrue(result);

        verify(listener).prePassivate(bean);
    }

    @Test
    public void removeBean() throws ClassNotFoundException, IOException {
        MarshalledValue<Map<String, Object>, MarshallingContext> value = mock(MarshalledValue.class);
        Map<String, Object> beans = mock(Map.class);
        String id = "id";
        Object bean = new Object();

        when(this.entry.getBeans()).thenReturn(value);
        when(value.get(this.context)).thenReturn(beans);
        when(beans.remove(id)).thenReturn(bean);

        Object result = this.group.removeBean(id);

        Assert.assertSame(bean, result);
    }

    @Test
    public void prePassivate() throws ClassNotFoundException, IOException {
        PassivationListener<Object> listener = mock(PassivationListener.class);
        MarshalledValue<Map<String, Object>, MarshallingContext> value = mock(MarshalledValue.class);
        Map<String, Object> beans = mock(Map.class);
        String id = "id";
        Object bean = new Object();

        this.group.prePassivate(id, null);

        verifyZeroInteractions(this.entry);

        when(this.entry.getBeans()).thenReturn(value);
        when(value.get(this.context)).thenReturn(beans);
        when(beans.get(id)).thenReturn(bean);

        this.group.prePassivate(id, listener);

        verify(listener).prePassivate(bean);
    }

    @Test
    public void postActivate() throws ClassNotFoundException, IOException {
        PassivationListener<Object> listener = mock(PassivationListener.class);
        MarshalledValue<Map<String, Object>, MarshallingContext> value = mock(MarshalledValue.class);
        Map<String, Object> beans = mock(Map.class);
        String id = "id";
        Object bean = new Object();

        this.group.postActivate(id, null);

        verifyZeroInteractions(this.entry);

        when(this.entry.getBeans()).thenReturn(value);
        when(value.get(this.context)).thenReturn(beans);
        when(beans.get(id)).thenReturn(bean);

        this.group.postActivate(id, listener);

        verify(listener).postActivate(bean);
    }

    @Test
    public void close() throws ClassNotFoundException, IOException {
        MarshalledValue<Map<String, Object>, MarshallingContext> value = mock(MarshalledValue.class);

        when(this.entry.getBeans()).thenReturn(value);
        when(value.get(this.context)).thenReturn(Collections.<String, Object>emptyMap());

        this.group.close();

        verify(this.remover).remove(this.id);
        verify(this.mutator, never()).mutate();

        reset(this.remover, this.mutator);

        when(value.get(this.context)).thenReturn(Collections.singletonMap("id", new Object()));

        this.group.close();

        verify(this.mutator).mutate();
        verify(this.remover, never()).remove(this.id);
    }
}
