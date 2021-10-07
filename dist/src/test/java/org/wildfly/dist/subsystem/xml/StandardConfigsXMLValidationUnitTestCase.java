/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.wildfly.dist.subsystem.xml;

import org.junit.Test;
import org.wildfly.test.distribution.validation.AbstractValidationUnitTest;

/**
 * Performs validation against their xsd of the server configuration files in the distribution.
 *
 * @author Brian Stansberry
 */
public class StandardConfigsXMLValidationUnitTestCase extends AbstractValidationUnitTest {

    @Test
    public void testHost() throws Exception {
        parseXml("domain/configuration/host.xml");
    }

    @Test
    public void testHostSlave() throws Exception {
        parseXml("domain/configuration/host-slave.xml");
    }

    @Test
    public void testHostMaster() throws Exception {
        parseXml("domain/configuration/host-master.xml");
    }

    @Test
    public void testDomain() throws Exception {
        parseXml("domain/configuration/domain.xml");
    }

    @Test
    public void testStandalone() throws Exception {
        parseXml("standalone/configuration/standalone.xml");
    }

    @Test
    public void testStandaloneHA() throws Exception {
        parseXml("standalone/configuration/standalone-ha.xml");
    }

    @Test
    public void testStandaloneFull() throws Exception {
        parseXml("standalone/configuration/standalone-full.xml");
    }

    @Test
    public void testStandaloneMicroProfile() throws Exception {
        parseXml("standalone/configuration/standalone-microprofile.xml");
    }

    @Test
    public void testStandaloneMicroProfileHA() throws Exception {
        parseXml("standalone/configuration/standalone-microprofile-ha.xml");
    }

    //TODO Leave commented out until domain-jts.xml is definitely removed from the configuration
//    @Test
//    public void testDomainJTS() throws Exception {
//        parseXml("docs/examples/configs/domain-jts.xml");
//    }
//
    @Test
    public void testStandaloneEC2HA() throws Exception {
        parseXml("docs/examples/configs/standalone-ec2-ha.xml");
    }

    @Test
    public void testStandaloneEC2FullHA() throws Exception {
        parseXml("docs/examples/configs/standalone-ec2-full-ha.xml");

    }
    @Test
    public void testStandaloneGossipHA() throws Exception {
        parseXml("docs/examples/configs/standalone-gossip-ha.xml");
    }

    @Test
    public void testStandaloneGossipFullHA() throws Exception {
        parseXml("docs/examples/configs/standalone-gossip-full-ha.xml");
    }

    @Test
    public void testStandaloneJTS() throws Exception {
        parseXml("docs/examples/configs/standalone-jts.xml");
    }

    @Test
    public void testStandaloneMinimalistic() throws Exception {
        parseXml("docs/examples/configs/standalone-minimalistic.xml");
    }

    @Test
    public void testStandaloneXTS() throws Exception {
        parseXml("docs/examples/configs/standalone-xts.xml");
    }

    @Test
    public void testStandaloneRTS() throws Exception {
        parseXml("docs/examples/configs/standalone-rts.xml");
    }

    @Test
    public void testStandaloneGenericJMS() throws Exception {
        parseXml("docs/examples/configs/standalone-genericjms.xml");
    }
}
