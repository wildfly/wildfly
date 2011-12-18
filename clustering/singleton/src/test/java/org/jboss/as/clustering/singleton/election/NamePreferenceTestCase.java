package org.jboss.as.clustering.singleton.election;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.as.clustering.ClusterNode;
import org.junit.Test;

public class NamePreferenceTestCase {
    @Test
    public void test() {
        Preference preference = new NamePreference("node1");

        ClusterNode node1 = mock(ClusterNode.class);
        ClusterNode node2 = mock(ClusterNode.class);

        when(node1.getName()).thenReturn("node1");
        when(node2.getName()).thenReturn("node2");

        assertTrue(preference.preferred(node1));
        assertFalse(preference.preferred(node2));
    }
}
