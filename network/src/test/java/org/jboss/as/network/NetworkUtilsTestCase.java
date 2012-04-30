/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.Assert;

import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class NetworkUtilsTestCase {

    @Test
    public void testFormatIPv6Test() {
        checkSameFormat("localhost");
        checkSameFormat("127.0.0.1");
        checkSameFormat("www.jboss.org");
        checkSameFormat("[::1]");
        checkSameFormat("[fe80::200:f8ff:fe21:67cf]");
        checkEqualFormat("[::1]", "::1");
        checkEqualFormat("[fe80::200:f8ff:fe21:67cf]", "fe80::200:f8ff:fe21:67cf");
    }

    @Test
    public void testFormatInetAddress() throws Exception{
        InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
        Assert.assertEquals("127.0.0.1", NetworkUtils.formatAddress(inetAddress));
        
        inetAddress = InetAddress.getByName("0:0:0:0:0:0:0:1");
        Assert.assertEquals("::1", NetworkUtils.formatAddress(inetAddress));
        
        inetAddress = InetAddress.getByName("fe80:0:0:0:f24d:a2ff:fe63:5766");
        Assert.assertEquals("fe80::f24d:a2ff:fe63:5766", NetworkUtils.formatAddress(inetAddress));
        
        inetAddress = InetAddress.getByName("1:0:0:1:0:0:0:1");
        Assert.assertEquals("1:0:0:1::1", NetworkUtils.formatAddress(inetAddress));
        
        inetAddress = InetAddress.getByName("1:0:0:1:1:0:0:1");
        Assert.assertEquals("1::1:1:0:0:1", NetworkUtils.formatAddress(inetAddress));
    }

    @Test
    public void testFormatSocketAddress() throws Exception {
        InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
        InetSocketAddress socketAddress = new InetSocketAddress(inetAddress,8000);
        Assert.assertEquals("127.0.0.1:8000", NetworkUtils.formatAddress(socketAddress));
        
        inetAddress = InetAddress.getByName("0:0:0:0:0:0:0:1");
        socketAddress = new InetSocketAddress(inetAddress,8000);
        Assert.assertEquals("[::1]:8000", NetworkUtils.formatAddress(socketAddress));
        
        inetAddress = InetAddress.getByName("fe80:0:0:0:f24d:a2ff:fe63:5766");
        socketAddress = new InetSocketAddress(inetAddress,8000);
        Assert.assertEquals("[fe80::f24d:a2ff:fe63:5766]:8000", NetworkUtils.formatAddress(socketAddress));
        
        inetAddress = InetAddress.getByName("1:0:0:1:0:0:0:1");
        socketAddress = new InetSocketAddress(inetAddress,8000);
        Assert.assertEquals("[1:0:0:1::1]:8000", NetworkUtils.formatAddress(socketAddress));
        
        inetAddress = InetAddress.getByName("1:0:0:1:1:0:0:1");
        socketAddress = new InetSocketAddress(inetAddress,8000);
        Assert.assertEquals("[1::1:1:0:0:1]:8000", NetworkUtils.formatAddress(socketAddress));
    }

    private void checkSameFormat(String nochange) {
        checkEqualFormat(nochange, nochange);
    }

    private void checkEqualFormat(String expected, String input) {
        Assert.assertEquals(expected, NetworkUtils.formatPossibleIpv6Address(input));
    }
}
