/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.domain.suites;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.cli.BasicOpsTestCase;
import org.jboss.as.test.integration.domain.management.cli.DataSourceTestCase;
import org.jboss.as.test.integration.domain.management.cli.DeployAllServerGroupsTestCase;
import org.jboss.as.test.integration.domain.management.cli.DeploySingleServerGroupTestCase;
import org.jboss.as.test.integration.domain.management.cli.JmsTestCase;
import org.jboss.as.test.integration.domain.management.cli.RolloutPlanTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses ({
    BasicOpsTestCase.class,
    DeployAllServerGroupsTestCase.class,
    DeploySingleServerGroupTestCase.class,
    JmsTestCase.class,
    DataSourceTestCase.class,
    RolloutPlanTestCase.class
})
public class CLITestSuite {

    private static DomainTestSupport domainSupport;

    public static final Map<String, String []> hostServers = new HashMap<String, String[]>();
    public static final Map<String, String> hostAddresses = new HashMap<String, String>();
    public static final Map<String, String []> serverGroups = new HashMap<String, String[]>();
    public static final Map<String, Integer> portOffsets = new HashMap<String, Integer>();
    public static final Map<String, String []> serverProfiles = new HashMap<String, String[]>();
    public static final Map<String, Boolean> serverStatus = new HashMap<String, Boolean>();
      
    @BeforeClass
    public static void initSuite() throws Exception {
        domainSupport = new DomainTestSupport(CLITestSuite.class.getSimpleName(),
                "domain-configs/domain-standard.xml", "host-configs/host-master.xml", "host-configs/host-slave.xml");
        domainSupport.start();

        hostServers.put("master", new String[] {"main-one", "main-two", "other-one"});
        hostServers.put("slave", new String[] {"main-three", "main-four", "other-two"});

        hostAddresses.put("master", domainSupport.masterAddress);
        hostAddresses.put("slave", domainSupport.slaveAddress);

        serverGroups.put("main-server-group", new String[] {"main-one", "main-two", "main-three", "main-four"});
        serverGroups.put("other-server-group", new String[] {"other-one", "other-two"});

        serverProfiles.put("default", new String[] {"main-server-group"});
        serverProfiles.put("ha", new String[] {"other-server-group"});

        portOffsets.put("main-one", 0);
        portOffsets.put("main-two", 150);
        portOffsets.put("other-one", 250);
        portOffsets.put("main-three", 350);
        portOffsets.put("main-four", 450);
        portOffsets.put("other-two", 550);

        serverStatus.put("main-one", true);
        serverStatus.put("main-two", false);
        serverStatus.put("main-three", true);
        serverStatus.put("main-four", false);
        serverStatus.put("other-one", false);
        serverStatus.put("other-two", true);
        
    }

    @AfterClass
    public static void tearDownSuite() {
        domainSupport.stop();
    }

}
