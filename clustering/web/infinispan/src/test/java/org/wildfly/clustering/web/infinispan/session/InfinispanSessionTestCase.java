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
package org.wildfly.clustering.web.infinispan.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.jboss.as.clustering.infinispan.invoker.Remover;
import org.junit.Test;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionMetaData;

public class InfinispanSessionTestCase {
    private final String id = "session";
    private final SessionMetaData metaData = mock(SessionMetaData.class);
    private final SessionAttributes attributes = mock(SessionAttributes.class);
    private final SessionContext context = mock(SessionContext.class);
    private final Mutator mutator = mock(Mutator.class);
    private final Remover<String> remover = mock(Remover.class);
    private final LocalContextFactory<Object> localContextFactory = mock(LocalContextFactory.class);
    private final AtomicReference<Object> localContextRef = new AtomicReference<>();

    private final Session<Object> session = new InfinispanSession<>(this.id, this.metaData, this.attributes, this.localContextRef, this.localContextFactory, this.context, this.mutator, this.remover);
    
    @Test
    public void getId() {
        assertSame(this.id, this.session.getId());
    }
    
    @Test
    public void getAttributes() {
        assertSame(this.attributes, this.session.getAttributes());
        
        this.session.invalidate();
        
        IllegalStateException exception = null;
        try {
            this.session.getAttributes();
        } catch (IllegalStateException e) {
            exception = e;
        }
        assertNotNull(exception);
    }
    
    @Test
    public void getMetaData() {
        assertSame(this.metaData, this.session.getMetaData());
        
        this.session.invalidate();
        
        IllegalStateException exception = null;
        try {
            this.session.getMetaData();
        } catch (IllegalStateException e) {
            exception = e;
        }
        assertNotNull(exception);
    }
    
    @Test
    public void getContext() {
        assertSame(this.context, this.session.getContext());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void invalidate() {
        this.session.invalidate();
        
        verify(this.remover).remove(this.id);
        
        reset(this.remover);
        
        IllegalStateException exception = null;
        try {
            this.session.invalidate();
        } catch (IllegalStateException e) {
            exception = e;
        }
        assertNotNull(exception);
        
        verify(this.remover, never()).remove(this.id);
    }
    
    @Test
    public void isValid() {
        assertTrue(this.session.isValid());
        
        this.session.invalidate();
        
        assertFalse(this.session.isValid());
    }
    
    @Test
    public void close() {
        this.session.close();
        
        verify(this.metaData).setLastAccessedTime(any(Date.class));
        verify(this.mutator).mutate();
        
        reset(this.metaData, this.mutator);
        
        // Verify that session is not mutated if invalid
        this.session.invalidate();
        
        verify(this.metaData, never()).setLastAccessedTime(any(Date.class));
        verify(this.mutator, never()).mutate();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getLocalContext() {
        Object expected = new Object();
        when(this.localContextFactory.createLocalContext()).thenReturn(expected);
        
        Object result = this.session.getLocalContext();
        
        assertSame(expected, result);
        
        reset(this.localContextFactory);
        
        result = this.session.getLocalContext();
        
        verifyZeroInteractions(this.localContextFactory);
        
        assertSame(expected, result);
    }
}
