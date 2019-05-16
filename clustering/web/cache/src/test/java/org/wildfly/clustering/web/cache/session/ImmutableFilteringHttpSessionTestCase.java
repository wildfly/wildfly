package org.wildfly.clustering.web.cache.session;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Unit test for {@link ImmutableFilteringHttpSession}.
 * @author Paul Ferraro
 */
public class ImmutableFilteringHttpSessionTestCase {
    private final ImmutableSession session = mock(ImmutableSession.class);
    private final ServletContext context = mock(ServletContext.class);
    private final FilteringHttpSession httpSession = new ImmutableFilteringHttpSession(this.session, this.context);

    @Test
    public void getListeners() {
        Object object1 = new Object();
        Object object2 = new Object();
        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);

        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(new HashSet<>(Arrays.asList("non-listener1", "non-listener2", "listener1", "listener2")));
        when(attributes.getAttribute("non-listener1")).thenReturn(object1);
        when(attributes.getAttribute("non-listener2")).thenReturn(object2);
        when(attributes.getAttribute("listener1")).thenReturn(listener1);
        when(attributes.getAttribute("listener2")).thenReturn(listener2);

        Map<String, Runnable> result = this.httpSession.getAttributes(Runnable.class);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.toString(), result.containsKey("listener1"));
        Assert.assertTrue(result.toString(), result.containsKey("listener2"));
        Assert.assertSame(listener1, result.get("listener1"));
        Assert.assertSame(listener2, result.get("listener2"));
    }

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
