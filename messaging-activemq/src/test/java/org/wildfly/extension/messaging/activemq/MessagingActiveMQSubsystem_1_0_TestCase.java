/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemInitialization;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *  * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc
 */
public class MessagingActiveMQSubsystem_1_0_TestCase extends AbstractSubsystemBaseTest {

    public MessagingActiveMQSubsystem_1_0_TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new JGroupsSubsystemInitialization();
    }

    @Override
    protected void compare(ModelNode model1, ModelNode model2) {
        model1.get(ModelDescriptionConstants.SUBSYSTEM).remove("jgroups");
        model2.get(ModelDescriptionConstants.SUBSYSTEM).remove("jgroups");
        super.compare(model1, model2);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_1_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-messaging-activemq_1_0.xsd";
    }

    @Override
    protected KernelServices standardSubsystemTest(String configId, boolean compareXml) throws Exception {
        return super.standardSubsystemTest(configId, false);
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("messaging.cluster.user.name", "myClusterUser");
        properties.put("messaging.cluster.user.password", "myClusterPassword");
        return properties;
    }

    /////////////////////////////////////////
    //  Tests for HA Policy Configuration  //
    /////////////////////////////////////////

    @Test
    public void testHAPolicyConfiguration() throws Exception {
        standardSubsystemTest("subsystem_1_0_ha-policy.xml", false);
    }
}
