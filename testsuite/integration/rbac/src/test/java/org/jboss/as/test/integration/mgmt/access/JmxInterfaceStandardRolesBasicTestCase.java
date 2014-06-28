/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.mgmt.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_USER;
import static org.junit.Assert.fail;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.management.interfaces.JmxManagementInterface;
import org.jboss.as.test.integration.management.interfaces.ManagementInterface;
import org.jboss.as.test.integration.management.rbac.RbacAdminCallbackHandler;
import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * @author jcechace
 * @author Ladislav Thon <lthon@redhat.com>
 */
@RunWith(Arquillian.class)
@ServerSetup(StandardUsersSetupTask.class)
public class JmxInterfaceStandardRolesBasicTestCase extends StandardRolesBasicTestCase {
    private static final String EXAMPLE_DS = "subsystem=datasources,data-source=ExampleDS";
    private static final String HTTP_SOCKET_BINDING = "socket-binding-group=standard-sockets,socket-binding=http";

    @Override
    protected ManagementInterface createClient(String userName) {
        return JmxManagementInterface.create(
                getManagementClient().getRemoteJMXURL(),
                userName, RbacAdminCallbackHandler.STD_PASSWORD,
                getJmxDomain()
        );
    }

    protected String getJmxDomain() {
        return "jboss.as";
    }

    @Override
    public void testMonitor() throws Exception {
        super.testMonitor();

        ManagementInterface client = getClientForUser(MONITOR_USER);
        //checkAttributeAccessInfo(client, true, false);
        //checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Override
    public void testOperator() throws Exception {
        super.testOperator();

        ManagementInterface client = getClientForUser(MONITOR_USER);
        //checkAttributeAccessInfo(client, true, false);
        //checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Override
    public void testMaintainer() throws Exception {
        super.testMaintainer();

        ManagementInterface client = getClientForUser(MONITOR_USER);
        checkAttributeAccessInfo(client, true, true);
        //checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Override
    public void testDeployer() throws Exception {
        super.testDeployer();

        ManagementInterface client = getClientForUser(MONITOR_USER);
        //checkAttributeAccessInfo(client, true, false);
        //checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Override
    public void testAdministrator() throws Exception {
        super.testAdministrator();

        ManagementInterface client = getClientForUser(MONITOR_USER);
        checkAttributeAccessInfo(client, true, true);
        checkSensitiveAttributeAccessInfo(client, true, true);
    }

    @Override
    public void testAuditor() throws Exception {
        super.testAuditor();

        ManagementInterface client = getClientForUser(MONITOR_USER);
        //checkAttributeAccessInfo(client, true, false);
        //checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Override
    public void testSuperUser() throws Exception {
        super.testSuperUser();

        ManagementInterface client = getClientForUser(MONITOR_USER);
        checkAttributeAccessInfo(client, true, true);
        checkSensitiveAttributeAccessInfo(client, true, true);
    }

    // test utils

    // TODO check[Sensitive]AttributeAccessInfo calls are mostly commented out because of https://issues.jboss.org/browse/WFLY-1984

    private void checkAttributeAccessInfo(ManagementInterface client, boolean read, boolean write) throws Exception {
        JmxManagementInterface jmxClient = (JmxManagementInterface) client;
        readAttributeAccessInfo(jmxClient, HTTP_SOCKET_BINDING, PORT, read, write);
    }

    private void checkSensitiveAttributeAccessInfo(ManagementInterface client, boolean read, boolean write) throws Exception {
        JmxManagementInterface jmxClient = (JmxManagementInterface) client;
        readAttributeAccessInfo(jmxClient, EXAMPLE_DS, PASSWORD, read, write);
    }

    private void readAttributeAccessInfo(JmxManagementInterface client, String address, String attribute,
                                         boolean read, boolean write) throws Exception {
        ObjectName objectName = new ObjectName(getJmxDomain() + ":" + address);
        MBeanInfo mBeanInfo = client.getConnection().getMBeanInfo(objectName);
        for (MBeanAttributeInfo attrInfo : mBeanInfo.getAttributes()) {
            if (attrInfo.getName().equals(attribute)) {
                Assert.assertEquals(read, attrInfo.isReadable());
                Assert.assertEquals(write, attrInfo.isWritable());
                return;
            }
        }
        fail("Attribute " + attribute + " not found at " + address);
    }
}
