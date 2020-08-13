package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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
import org.wildfly.clustering.web.session.HttpSessionFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Unit test for {@link HttpSessionFactory}.
 * @author Paul Ferraro
 */
public class UndertowHttpSessionFactoryTestCase {
    private final HttpSessionFactory<HttpSession, ServletContext> factory = UndertowSpecificationProvider.INSTANCE;

    @Test
    public void getId() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);

        String expected = "session";
        when(session.getId()).thenReturn(expected);

        String result = this.factory.createHttpSession(session, context).getId();

        assertSame(expected, result);
    }

    @Test
    public void getCreationTime() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        Instant now = Instant.now();
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(now);

        long result = this.factory.createHttpSession(session, context).getCreationTime();

        assertEquals(now.toEpochMilli(), result);
    }

    @Test
    public void getLastAccessedTime() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        Instant now = Instant.now();
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessedTime()).thenReturn(now);

        long result = this.factory.createHttpSession(session, context).getLastAccessedTime();

        assertEquals(now.toEpochMilli(), result);
    }

    @Test
    public void getMaxInactiveInterval() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        Duration interval = Duration.of(100L, ChronoUnit.SECONDS);

        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getMaxInactiveInterval()).thenReturn(interval);

        int result = this.factory.createHttpSession(session, context).getMaxInactiveInterval();

        assertEquals(interval.getSeconds(), result);
    }

    @Test
    public void setMaxInactiveInterval() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);

        this.factory.createHttpSession(session, context).setMaxInactiveInterval(10);

        verifyZeroInteractions(session);
    }

    @Test
    public void getServletContext() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);

        ServletContext result = this.factory.createHttpSession(session, context).getServletContext();

        assertSame(context, result);
    }

    @Test
    public void getAttributeNames() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Set<String> expected = new TreeSet<>();

        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(expected);

        Enumeration<String> result = this.factory.createHttpSession(session, context).getAttributeNames();

        assertEquals(new ArrayList<>(expected), Collections.list(result));
    }

    @Test
    public void getAttribute() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        String name = "name";
        Object expected = new Object();

        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttribute(name)).thenReturn(expected);

        Object result = this.factory.createHttpSession(session, context).getAttribute(name);

        assertSame(expected, result);
    }

    @Test
    public void setAttribute() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);

        this.factory.createHttpSession(session, context).setAttribute("name", "value");

        verifyZeroInteractions(session);
    }

    @Test
    public void removeAttribute() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);

        this.factory.createHttpSession(session, context).removeAttribute("name");

        verifyZeroInteractions(session);
    }

    @Test
    public void invalidate() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);

        this.factory.createHttpSession(session, context).invalidate();

        verifyZeroInteractions(session);
    }

    @Test
    public void isNew() {
        ImmutableSession session = mock(ImmutableSession.class);
        ServletContext context = mock(ServletContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);

        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isNew()).thenReturn(true, false);

        assertTrue(this.factory.createHttpSession(session, context).isNew());
        assertFalse(this.factory.createHttpSession(session, context).isNew());
    }
}
