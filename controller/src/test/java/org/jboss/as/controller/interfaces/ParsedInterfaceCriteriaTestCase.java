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
package org.jboss.as.controller.interfaces;

import java.net.Inet4Address;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.jboss.as.controller.parsing.Element;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ParsedInterfaceCriteriaTestCase {

    //TODO
//    @Test
//    public void testNotLoopbackAndIntetaddress() {
//        ModelNode op = new ModelNode();
//        op.get(Element.LOOPBACK.getLocalName()).set(true);
//        op.get(Element.INET_ADDRESS.getLocalName()).set("127.0.0.1");
//        ParsedInterfaceCriteria criteria = ParsedInterfaceCriteria.parse(op);
//        Assert.assertNotNull(criteria.getFailureMessage());
//    }

    @Test
    public void testNotAndSameIncludedCriteria() {
        ModelNode op = new ModelNode();
        op.get(Element.LOOPBACK.getLocalName()).set(true);
        op.get(Element.NOT.getLocalName(), Element.LOOPBACK.getLocalName()).set(true);
        ParsedInterfaceCriteria criteria = ParsedInterfaceCriteria.parse(op, true);
        Assert.assertNotNull(criteria.getFailureMessage());
    }

    @Test
    public void testNotAndDifferentIncludedCriteria() {
        ModelNode op = new ModelNode();
        op.get(Element.LOOPBACK.getLocalName()).set(true);
        op.get(Element.NOT.getLocalName(), Element.INET_ADDRESS.getLocalName()).set("127.0.0.1");
        ParsedInterfaceCriteria criteria = ParsedInterfaceCriteria.parse(op, true);
        Assert.assertNull(criteria.getFailureMessage());
    }

    @Test
    public void testEqualsMethods() throws Exception {
        //The 'not' part of the validation uses equals so test it properly here
        //TODO Any
        //TODO Not
        Assert.assertTrue(new InetAddressMatchInterfaceCriteria("127.0.0.1").equals(new InetAddressMatchInterfaceCriteria("127.0.0.1")));
        Assert.assertFalse(new InetAddressMatchInterfaceCriteria("127.0.0.1").equals(new InetAddressMatchInterfaceCriteria("127.0.0.2")));
        Assert.assertTrue(new InetAddressMatchInterfaceCriteria(new ModelNode("127.0.0.1")).equals(new InetAddressMatchInterfaceCriteria(new ModelNode("127.0.0.1"))));
        Assert.assertFalse(new InetAddressMatchInterfaceCriteria(new ModelNode("127.0.0.1")).equals(new InetAddressMatchInterfaceCriteria(new ModelNode("127.0.0.2"))));
        Assert.assertTrue(LinkLocalInterfaceCriteria.INSTANCE.equals(LinkLocalInterfaceCriteria.INSTANCE));
        Assert.assertTrue(new LoopbackAddressInterfaceCriteria(new ModelNode("127.0.0.1")).equals(new LoopbackAddressInterfaceCriteria(new ModelNode("127.0.0.1"))));
        Assert.assertFalse(new LoopbackAddressInterfaceCriteria(new ModelNode("127.0.0.1")).equals(new LoopbackAddressInterfaceCriteria(new ModelNode("127.0.0.2"))));
        Assert.assertTrue(LoopbackInterfaceCriteria.INSTANCE.equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertTrue(new NicInterfaceCriteria("en1").equals(new NicInterfaceCriteria("en1")));
        Assert.assertFalse(new NicInterfaceCriteria("en1").equals(new NicInterfaceCriteria("en2")));
        Assert.assertFalse(new NicMatchInterfaceCriteria(Pattern.compile(".")).equals(new NicMatchInterfaceCriteria(Pattern.compile("a"))));
        Assert.assertTrue(new NicMatchInterfaceCriteria(Pattern.compile(".")).equals(new NicMatchInterfaceCriteria(Pattern.compile("."))));
        Assert.assertFalse(new NicMatchInterfaceCriteria(Pattern.compile(".")).equals(new NicMatchInterfaceCriteria(Pattern.compile("a"))));
        Assert.assertTrue(PointToPointInterfaceCriteria.INSTANCE.equals(PointToPointInterfaceCriteria.INSTANCE));
        Assert.assertTrue(SiteLocalInterfaceCriteria.INSTANCE.equals(SiteLocalInterfaceCriteria.INSTANCE));
        Assert.assertTrue(new SubnetMatchInterfaceCriteria(new byte[] {1, 2}, 3).equals(new SubnetMatchInterfaceCriteria(new byte[] {1, 2}, 3)));
        Assert.assertFalse(new SubnetMatchInterfaceCriteria(new byte[] {1, 2}, 3).equals(new SubnetMatchInterfaceCriteria(new byte[] {2}, 3)));
        Assert.assertFalse(new SubnetMatchInterfaceCriteria(new byte[] {1, 2}, 3).equals(new SubnetMatchInterfaceCriteria(new byte[] {1, 2}, 4)));
        Assert.assertTrue(SupportsMulticastInterfaceCriteria.INSTANCE.equals(SupportsMulticastInterfaceCriteria.INSTANCE));
        Assert.assertTrue(UpInterfaceCriteria.INSTANCE.equals(UpInterfaceCriteria.INSTANCE));
        Assert.assertTrue(VirtualInterfaceCriteria.INSTANCE.equals(VirtualInterfaceCriteria.INSTANCE));
        Assert.assertTrue(new WildcardInetAddressInterfaceCriteria(Inet4Address.getLocalHost()).equals(new WildcardInetAddressInterfaceCriteria(Inet4Address.getLocalHost())));


        Assert.assertFalse(new InetAddressMatchInterfaceCriteria("127.0.0.1").equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(new InetAddressMatchInterfaceCriteria(new ModelNode("127.0.0.1")).equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(LinkLocalInterfaceCriteria.INSTANCE.equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(new LoopbackAddressInterfaceCriteria(new ModelNode("127.0.0.1")).equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(new NicInterfaceCriteria("en1").equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(new NicMatchInterfaceCriteria(Pattern.compile(".")).equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(PointToPointInterfaceCriteria.INSTANCE.equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(SiteLocalInterfaceCriteria.INSTANCE.equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(new SubnetMatchInterfaceCriteria(new byte[] {1, 2}, 3).equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(SupportsMulticastInterfaceCriteria.INSTANCE.equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(UpInterfaceCriteria.INSTANCE.equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(VirtualInterfaceCriteria.INSTANCE.equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(new WildcardInetAddressInterfaceCriteria(Inet4Address.getLocalHost()).equals(LoopbackInterfaceCriteria.INSTANCE));
        Assert.assertFalse(LoopbackInterfaceCriteria.INSTANCE.equals(UpInterfaceCriteria.INSTANCE));
    }
}
