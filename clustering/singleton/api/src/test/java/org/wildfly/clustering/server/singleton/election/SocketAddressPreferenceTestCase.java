package org.wildfly.clustering.server.singleton.election;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Test;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.election.Preference;
import org.wildfly.clustering.singleton.election.SocketAddressPreference;

public class SocketAddressPreferenceTestCase {
    @Test
    public void test() throws UnknownHostException {
        InetSocketAddress preferredAddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 1);
        InetSocketAddress otherAddress1 = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 2);
        InetSocketAddress otherAddress2 = new InetSocketAddress(InetAddress.getByName("127.0.0.2"), 1);

        Preference preference = new SocketAddressPreference(preferredAddress);

        Node preferredNode = mock(Node.class);
        Node otherNode1 = mock(Node.class);
        Node otherNode2 = mock(Node.class);

        when(preferredNode.getSocketAddress()).thenReturn(preferredAddress);
        when(otherNode1.getSocketAddress()).thenReturn(otherAddress1);
        when(otherNode2.getSocketAddress()).thenReturn(otherAddress2);

        assertTrue(preference.preferred(preferredNode));
        assertFalse(preference.preferred(otherNode1));
        assertFalse(preference.preferred(otherNode2));
    }
}
