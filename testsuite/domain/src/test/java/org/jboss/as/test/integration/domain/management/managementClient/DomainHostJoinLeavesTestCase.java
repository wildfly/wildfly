/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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


package org.jboss.as.test.integration.domain.management;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.domain.ManagementClient;
import org.jboss.logging.Logger;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Panagiotis Sotiropoulos (c) 2014 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class DomainHostJoinLeavesTestCase {
    private static DomainLifecycleUtil slaveDomainLifecycleUtil;
    private static DomainLifecycleUtil masterDomainLifecycleUtil;

    private static ModelControllerClient domainMasterClient;
    private static ModelControllerClient domainSlaveClient;
    private static DomainTestSupport testSupport;

    private static ManagementClient client;
    private HashSet<String> joinedNewHosts;
    private HashSet<String> leftNewHosts;
    private static InetAddress address = InetAddress.getLoopbackAddress();
    private static int port = 9990;
    Logger log = Logger.getLogger("DomainHostJoinLeavesTestCase");

    @BeforeClass
    public static void setupDomain() throws Exception {

        testSupport = DomainTestSupport.create(
                DomainTestSupport.Configuration.create(DomainHostJoinLeavesTestCase.class.getSimpleName(),
                        "domain-configs/domain.xml",
                        "host-configs/host-master.xml", "host-configs/host-slave2.xml"));

        testSupport.start();

        masterDomainLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        slaveDomainLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();
        domainMasterClient = masterDomainLifecycleUtil.getDomainClient();
        domainSlaveClient = slaveDomainLifecycleUtil.getDomainClient();

        client = new ManagementClient(domainMasterClient, address.getHostAddress(), port);

    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        slaveDomainLifecycleUtil.setCloseConnectionBeforeStop(true);
        testSupport.stop();
        testSupport = null;
        domainMasterClient = null;
        domainSlaveClient = null;

        if (client != null) {
            client.close();
        }
    }


    @Test
    public void testHostJoinLeave() throws Exception {
        Set<String> currentHosts = client.getDomainHosts();
        Set<String> hosts;

        hosts = client.getDomainHosts();
        getHostDifference(currentHosts, hosts);
        currentHosts = hosts;
        Assert.assertTrue(currentHosts.toString(), currentHosts.toString().contains("slave"));

        slaveDomainLifecycleUtil.stop();
        hosts = client.getDomainHosts();
        getHostDifference(currentHosts, hosts);
        currentHosts = hosts;
        Assert.assertFalse(currentHosts.toString(), currentHosts.toString().contains("slave"));

        slaveDomainLifecycleUtil.start();
        hosts = client.getDomainHosts();
        getHostDifference(currentHosts, hosts);
        currentHosts = hosts;
        Assert.assertTrue(currentHosts.toString(), currentHosts.toString().contains("slave"));
    }


    private void getHostDifference(Set<String> previousHosts, Set<String> hosts) {
        if (hosts.toString().compareTo(previousHosts.toString()) != 0) {
            log.info("Hosts of Domain have been changed ... ");

            if (hosts.containsAll(previousHosts)) {
                joinedNewHosts = new HashSet<String>(hosts);
                joinedNewHosts.removeAll(previousHosts);
                log.info("Hosts have joined the domain ... " + joinedNewHosts.toString());
            } else if (previousHosts.containsAll(hosts)) {
                leftNewHosts = new HashSet<String>(previousHosts);
                leftNewHosts.removeAll(hosts);
                log.info("Hosts have left the domain ... " + leftNewHosts.toString());
            } else {
                log.info("Hosts have joined and left the domain ... ");
                joinedNewHosts = new HashSet<String>(hosts);
                joinedNewHosts.removeAll(previousHosts);
                leftNewHosts = new HashSet<String>(previousHosts);
                leftNewHosts.removeAll(hosts);
                log.info("Hosts have joined the domain ... " + joinedNewHosts.toString());
                log.info("Hosts have left the domain ... " + leftNewHosts.toString());
            }
        }
    }


}

