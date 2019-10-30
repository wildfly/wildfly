package org.wildfly.clustering.server.singleton.election;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.election.NamePreference;
import org.wildfly.clustering.singleton.election.Preference;

public class NamePreferenceTestCase {
    @Test
    public void test() {
        Preference preference = new NamePreference("node1");

        Node node1 = mock(Node.class);
        Node node2 = mock(Node.class);

        when(node1.getName()).thenReturn("node1");
        when(node2.getName()).thenReturn("node2");

        assertTrue(preference.preferred(node1));
        assertFalse(preference.preferred(node2));
    }
}
