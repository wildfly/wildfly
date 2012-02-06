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
package org.jboss.as.protocol.mgmt;

import junit.framework.Assert;

import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ProtocolUtilsTestCase {

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

    private void checkSameFormat(String nochange) {
        checkEqualFormat(nochange, nochange);
    }

    private void checkEqualFormat(String expected, String input) {
        Assert.assertEquals(expected, ProtocolUtils.formatPossibleIpv6Address(input));
    }
}
