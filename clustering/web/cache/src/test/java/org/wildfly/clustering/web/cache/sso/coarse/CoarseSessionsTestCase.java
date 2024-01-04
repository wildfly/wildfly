/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.sso.coarse;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.web.sso.Sessions;

public class CoarseSessionsTestCase {
    private Mutator mutator = mock(Mutator.class);
    private Map<String, String> map = mock(Map.class);
    private Sessions<String, String> sessions = new CoarseSessions<>(this.map, this.mutator);

    @Test
    public void getApplications() {
        Set<String> expected = Collections.singleton("deployment");
        when(this.map.keySet()).thenReturn(expected);

        Set<String> result = this.sessions.getDeployments();

        assertEquals(expected, result);

        verify(this.mutator, never()).mutate();
    }

    @Test
    public void getSession() {
        String expected = "id";
        String deployment = "deployment1";
        String missingDeployment = "deployment2";

        when(this.map.get(deployment)).thenReturn(expected);
        when(this.map.get(missingDeployment)).thenReturn(null);

        assertSame(expected, this.sessions.getSession(deployment));
        assertNull(this.sessions.getSession(missingDeployment));

        verify(this.mutator, never()).mutate();
    }

    @Test
    public void addSession() {
        String id = "id";
        String deployment = "deployment";

        when(this.map.put(deployment, id)).thenReturn(null);

        this.sessions.addSession(deployment, id);

        verify(this.mutator).mutate();

        reset(this.map, this.mutator);

        when(this.map.put(deployment, id)).thenReturn(id);

        this.sessions.addSession(deployment, id);

        verify(this.mutator, never()).mutate();
    }

    @Test
    public void removeSession() {
        String deployment = "deployment";

        when(this.map.remove(deployment)).thenReturn("id");

        this.sessions.removeSession(deployment);

        verify(this.mutator).mutate();

        reset(this.map, this.mutator);

        when(this.map.remove(deployment)).thenReturn(null);

        this.sessions.removeSession(deployment);

        verify(this.mutator, never()).mutate();
    }
}
