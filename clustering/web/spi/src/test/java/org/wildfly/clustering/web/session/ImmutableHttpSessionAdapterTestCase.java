package org.wildfly.clustering.web.session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.junit.Test;

public class ImmutableHttpSessionAdapterTestCase {
    private final ImmutableSession session = mock(ImmutableSession.class);
    private final ServletContext context = mock(ServletContext.class);
    private final HttpSession httpSession = new ImmutableHttpSessionAdapter(this.session, this.context);

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
        Instant now = Instant.now();
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(now);

        long result = this.httpSession.getCreationTime();

        assertEquals(now.toEpochMilli(), result);
    }

    @Test
    public void getLastAccessedTime() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        Instant now = Instant.now();
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessedTime()).thenReturn(now);

        long result = this.httpSession.getLastAccessedTime();

        assertEquals(now.toEpochMilli(), result);
    }

    @Test
    public void getMaxInactiveInterval() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        Duration interval = Duration.of(100L, ChronoUnit.SECONDS);

        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getMaxInactiveInterval()).thenReturn(interval);

        int result = this.httpSession.getMaxInactiveInterval();

        assertEquals(interval.getSeconds(), result);
    }

    @Test
    public void setMaxInactiveInterval() {
        this.httpSession.setMaxInactiveInterval(10);

        verifyZeroInteractions(this.session);
    }

    @Test
    public void getServletContext() {
        assertSame(this.context, this.httpSession.getServletContext());
    }

    @Test
    public void getAttributeNames() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        Set<String> expected = new TreeSet<>();

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
        this.httpSession.setAttribute("name", "value");

        verifyZeroInteractions(this.session);
    }

    @Test
    public void removeAttribute() {
        this.httpSession.removeAttribute("name");

        verifyZeroInteractions(this.session);
    }

    @Test
    public void invalidate() {
        this.httpSession.invalidate();

        verifyZeroInteractions(this.session);
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
