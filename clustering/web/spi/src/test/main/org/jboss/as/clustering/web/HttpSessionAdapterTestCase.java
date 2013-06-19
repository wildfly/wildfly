package org.jboss.as.clustering.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class HttpSessionAdapterTestCase {
    private final Session session = mock(Session.class);
    private final HttpSession httpSession = new HttpSessionAdapter(this.session);
    
    @Test
    public void getId() {
        String expected = "session";
        when(this.session.getId()).thenReturn(expected);
        
        String result = this.httpSession.getId();
        
        assertSame(expected, result);
    }
    
    @Test
    public void getCreationTime() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        Date date = new Date();
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(date);
        
        long result = this.httpSession.getCreationTime();
        
        assertEquals(date.getTime(), result);
    }
    
    @Test
    public void getLastAccessedTime() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        Date date = new Date();
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessedTime()).thenReturn(date);
        
        long result = this.httpSession.getLastAccessedTime();
        
        assertEquals(date.getTime(), result);
    }
    
    @Test
    public void getMaxInactiveInterval() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        int expected = 100;
        
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getMaxInactiveInterval(TimeUnit.SECONDS)).thenReturn(Long.valueOf(expected));
        
        int result = this.httpSession.getMaxInactiveInterval();
        
        assertEquals(expected, result);
    }
    
    @Test
    public void setMaxInactiveInterval() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        int expected = 100;
        
        when(this.session.getMetaData()).thenReturn(metaData);

        this.httpSession.setMaxInactiveInterval(expected);

        verify(metaData).setMaxInactiveInterval(expected, TimeUnit.SECONDS);
    }
    
    @Test
    public void getServletContext() {
        SessionContext context = mock(SessionContext.class);
        ServletContext expected = mock(ServletContext.class);
        
        when(this.session.getContext()).thenReturn(context);
        when(context.getServletContext()).thenReturn(expected);
        
        ServletContext result = this.httpSession.getServletContext();
        
        assertSame(expected, result);
    }
    
    @Test
    public void getAttributeNames() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        Set<String> expected = new TreeSet<String>();
        
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(expected);
        
        Enumeration<String> result = this.httpSession.getAttributeNames();
        
        assertEquals(new ArrayList<>(expected), Collections.list(result));
    }

    @Test
    public void getAttribute() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        String name = "name";
        Object expected = new Object();
        
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttribute(name)).thenReturn(expected);
        
        Object result = this.httpSession.getAttribute(name);
        
        assertSame(expected, result);
    }
    
    @Test
    public void setAttribute() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionContext context = mock(SessionContext.class);
        HttpSessionAttributeListener listener = mock(HttpSessionAttributeListener.class);
        String addedName = "added";
        HttpSessionBindingListener addedValue = mock(HttpSessionBindingListener.class);
        
        when(this.session.getAttributes()).thenReturn(attributes);
        when(this.session.getContext()).thenReturn(context);
        when(context.getSessionAttributeListeners()).thenReturn(Collections.singleton(listener));
        
        // Test added attribute
        when(attributes.setAttribute(addedName, addedValue)).thenReturn(null);
        
        this.httpSession.setAttribute(addedName, addedValue);
        
        ArgumentCaptor<HttpSessionBindingEvent> capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(listener).attributeAdded(capturedEvent.capture());
        HttpSessionBindingEvent event = capturedEvent.getValue();
        assertSame(event.getSession(), this.httpSession);
        assertSame(event.getName(), addedName);
        assertSame(event.getValue(), addedValue);
        
        capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(addedValue).valueBound(capturedEvent.capture());
        event = capturedEvent.getValue();
        assertSame(event.getSession(), this.httpSession);
        assertSame(event.getName(), addedName);
        assertNull(event.getValue());

        verify(listener, never()).attributeReplaced(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeRemoved(any(HttpSessionBindingEvent.class));
        verify(addedValue, never()).valueUnbound(any(HttpSessionBindingEvent.class));
        reset(listener);
        
        // Test replaced attribute
        String replacedName = "replaced";
        HttpSessionBindingListener replacedNewValue = mock(HttpSessionBindingListener.class);
        HttpSessionBindingListener replacedOldValue = mock(HttpSessionBindingListener.class);
        
        when(attributes.setAttribute(replacedName, replacedNewValue)).thenReturn(replacedOldValue);

        this.httpSession.setAttribute(replacedName, replacedNewValue);
        
        capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(listener).attributeReplaced(capturedEvent.capture());
        event = capturedEvent.getValue();
        assertSame(event.getSession(), this.httpSession);
        assertSame(event.getName(), replacedName);
        assertSame(event.getValue(), replacedOldValue);
        
        capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(replacedNewValue).valueBound(capturedEvent.capture());
        event = capturedEvent.getValue();
        assertSame(event.getSession(), this.httpSession);
        assertSame(event.getName(), replacedName);
        assertNull(event.getValue());
        
        capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(replacedOldValue).valueUnbound(capturedEvent.capture());
        event = capturedEvent.getValue();
        assertSame(event.getSession(), this.httpSession);
        assertSame(event.getName(), replacedName);
        assertNull(event.getValue());

        verify(listener, never()).attributeAdded(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeRemoved(any(HttpSessionBindingEvent.class));
        verify(replacedNewValue, never()).valueUnbound(any(HttpSessionBindingEvent.class));
        verify(replacedOldValue, never()).valueBound(any(HttpSessionBindingEvent.class));
        reset(listener);
        
        // Test removed attribute
        String removedName = "removed";
        HttpSessionBindingListener removedValue = mock(HttpSessionBindingListener.class);

        when(attributes.setAttribute(removedName, null)).thenReturn(removedValue);

        this.httpSession.setAttribute(removedName, null);
        
        capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(listener).attributeRemoved(capturedEvent.capture());
        event = capturedEvent.getValue();
        assertSame(event.getSession(), this.httpSession);
        assertSame(event.getName(), removedName);
        assertSame(event.getValue(), removedValue);
        
        capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(removedValue).valueUnbound(capturedEvent.capture());
        event = capturedEvent.getValue();
        assertSame(event.getSession(), this.httpSession);
        assertSame(event.getName(), removedName);
        assertNull(event.getValue());

        verify(listener, never()).attributeAdded(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeReplaced(any(HttpSessionBindingEvent.class));
        verify(removedValue, never()).valueBound(any(HttpSessionBindingEvent.class));
        reset(listener);
        
        // Test same attribute
        String sameName = "same";
        HttpSessionBindingListener sameValue = mock(HttpSessionBindingListener.class);
        
        when(attributes.setAttribute(sameName, sameValue)).thenReturn(sameValue);
        
        this.httpSession.setAttribute(sameName, sameValue);
        
        verify(listener, never()).attributeAdded(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeReplaced(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeRemoved(any(HttpSessionBindingEvent.class));
        verify(sameValue, never()).valueUnbound(any(HttpSessionBindingEvent.class));
        verify(sameValue, never()).valueBound(any(HttpSessionBindingEvent.class));
        reset(listener);
        
        // Test null attribute
        String nullName = "null";
        
        when(attributes.setAttribute(nullName, null)).thenReturn(null);
        
        this.httpSession.setAttribute(nullName, null);

        verify(listener, never()).attributeAdded(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeReplaced(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeRemoved(any(HttpSessionBindingEvent.class));
        reset(listener);
    }
    
    @Test
    public void removeAttribute() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionContext context = mock(SessionContext.class);
        HttpSessionAttributeListener listener = mock(HttpSessionAttributeListener.class);
        String removedName = "removed";
        HttpSessionBindingListener removedValue = mock(HttpSessionBindingListener.class);
        
        when(this.session.getAttributes()).thenReturn(attributes);
        when(this.session.getContext()).thenReturn(context);
        when(context.getSessionAttributeListeners()).thenReturn(Collections.singleton(listener));
        
        // Test removed attribute
        when(attributes.setAttribute(removedName, null)).thenReturn(removedValue);

        this.httpSession.setAttribute(removedName, null);
        
        ArgumentCaptor<HttpSessionBindingEvent> capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(listener).attributeRemoved(capturedEvent.capture());
        HttpSessionBindingEvent event = capturedEvent.getValue();
        assertSame(event.getSession(), this.httpSession);
        assertSame(event.getName(), removedName);
        assertSame(event.getValue(), removedValue);
        
        capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(removedValue).valueUnbound(capturedEvent.capture());
        event = capturedEvent.getValue();
        assertSame(event.getSession(), this.httpSession);
        assertSame(event.getName(), removedName);
        assertNull(event.getValue());

        verify(listener, never()).attributeAdded(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeReplaced(any(HttpSessionBindingEvent.class));
        verify(removedValue, never()).valueBound(any(HttpSessionBindingEvent.class));
        reset(listener);
        
        // Test null attribute
        String nullName = "null";

        when(attributes.setAttribute(nullName, null)).thenReturn(null);
        
        this.httpSession.setAttribute(nullName, null);

        verify(listener, never()).attributeAdded(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeReplaced(any(HttpSessionBindingEvent.class));
        verify(listener, never()).attributeRemoved(any(HttpSessionBindingEvent.class));
        reset(listener);
    }

    @Test
    public void invalidate() {
        this.httpSession.invalidate();
        
        verify(this.session).invalidate();
    }
    
    @Test
    public void isNew() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.isNew()).thenReturn(true, false);
        
        assertTrue(this.httpSession.isNew());
        assertFalse(this.httpSession.isNew());
    }
}
