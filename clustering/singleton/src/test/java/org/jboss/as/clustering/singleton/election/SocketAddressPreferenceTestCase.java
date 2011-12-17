package org.jboss.as.clustering.singleton.election;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.jboss.as.clustering.ClusterNode;
import org.junit.Test;

public class SocketAddressPreferenceTestCase {
    @Test
    public void test() throws UnknownHostException {
        InetAddress address1 = InetAddress.getByName("127.0.0.1");
        InetAddress address2 = InetAddress.getByName("127.0.0.2");

        Preference preference = new SocketAddressPreference(new InetSocketAddress(address1, 1));

        ClusterNode node1 = mock(ClusterNode.class);
        ClusterNode node2 = mock(ClusterNode.class);
        ClusterNode node3 = mock(ClusterNode.class);

        when(node1.getIpAddress()).thenReturn(address1);
        when(node2.getIpAddress()).thenReturn(address2);
        when(node3.getIpAddress()).thenReturn(address1);
        when(node1.getPort()).thenReturn(1);
        when(node2.getPort()).thenReturn(1);
        when(node3.getPort()).thenReturn(2);

        assertTrue(preference.preferred(node1));
        assertFalse(preference.preferred(node2));
        assertFalse(preference.preferred(node3));
    }
}
