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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.management.AccessConstraintKey;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.jdr.JdrReportExtension;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * Test of the access constraint utilization resources.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AccessConstraintUtilizationTestCase extends AbstractRbacTestCase {

    private static class ExpectedDef {
        private final AccessConstraintKey key;
        private final boolean expectResource;
        private final boolean expectAttributes;
        private final boolean expectOps;

        private ExpectedDef(AccessConstraintKey key, boolean expectResource, boolean expectAttributes, boolean expectOps) {
            this.key = key;
            this.expectResource = expectResource;
            this.expectAttributes = expectAttributes;
            this.expectOps = expectOps;
        }
    }

    private static final String ADDR_FORMAT =
            "core-service=management/access=authorization/constraint=%s/type=%s/classification=%s";
    private static final ExpectedDef[] EXPECTED_DEFS = {
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.ACCESS_CONTROL.getKey(), true, true, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.CREDENTIAL.getKey(), false, true, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.EXTENSIONS.getKey(), true, false, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.MANAGEMENT_INTERFACES.getKey(), true, false, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.MODULE_LOADING.getKey(), true, false, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.PATCHING.getKey(), true, false, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG.getKey(), false, false, true),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN.getKey(), true, false, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF.getKey(), false, true, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM.getKey(), true, false, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF.getKey(), false, true, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SECURITY_VAULT.getKey(), true, false, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SERVICE_CONTAINER.getKey(), true, false, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF.getKey(), false, true, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG.getKey(), true, true, true),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SNAPSHOTS.getKey(), false, false, true),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.SYSTEM_PROPERTY.getKey(), true, true, true),
        // A few subsystem ones
        new ExpectedDef(getSensKey(UndertowExtension.SUBSYSTEM_NAME, "web-access-log"), true, false, false),
        new ExpectedDef(getSensKey(DataSourcesExtension.SUBSYSTEM_NAME, "data-source-security"), false, true, false),
        new ExpectedDef(getSensKey(ResourceAdaptersExtension.SUBSYSTEM_NAME, "resource-adapter-security"), false, true, false),
        new ExpectedDef(getSensKey(JdrReportExtension.SUBSYSTEM_NAME, "jdr"), false, false, true),
        new ExpectedDef(getSensKey(MessagingExtension.SUBSYSTEM_NAME, "messaging-management"), false, true, false),
        /* N/A on standalone
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.DOMAIN_CONTROLLER, false, true, true),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.DOMAIN_NAMES, false, true, false),
        new ExpectedDef(SensitiveTargetAccessConstraintDefinition.JVM, false, true, true),
        */
        new ExpectedDef(ApplicationTypeAccessConstraintDefinition.DEPLOYMENT.getKey(), true, false, true),
        new ExpectedDef(getAppKey(DataSourcesExtension.SUBSYSTEM_NAME, "data-source"), true, false, false),
        new ExpectedDef(getAppKey(DataSourcesExtension.SUBSYSTEM_NAME, "xa-data-source"), true, false, false),
        new ExpectedDef(getAppKey(DataSourcesExtension.SUBSYSTEM_NAME, "jdbc-driver"), true, false, false),
        new ExpectedDef(getAppKey(MessagingExtension.SUBSYSTEM_NAME, "queue"), true, false, false),
        new ExpectedDef(getAppKey(MessagingExtension.SUBSYSTEM_NAME, "jms-queue"), true, false, false),
        new ExpectedDef(getAppKey(MessagingExtension.SUBSYSTEM_NAME, "jms-topic"), true, false, false),
        // Agroal
        new ExpectedDef(getAppKey("datasources-agroal", "datasource"), true, false, false),
        new ExpectedDef(getAppKey("datasources-agroal", "xa-datasource"), true, false, false),
        new ExpectedDef(getAppKey("datasources-agroal", "driver"), true, false, false)

    };

    @ContainerResource
    private ManagementClient managementClient;

    @Before
    public void addAgroal() throws IOException {
        ModelNode addOp = Util.createAddOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.datasources-agroal"));
        ModelNode response = managementClient.getControllerClient().execute(addOp);
        Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
    }

    @After
    public void removeAgroal() throws IOException {
        ModelNode removeOp = Util.createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.datasources-agroal"));
        ModelNode response = managementClient.getControllerClient().execute(removeOp);
        Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
    }

    @Test
    public void testConstraintUtilization() throws Exception {
        ModelControllerClient client = managementClient.getControllerClient();
        for (ExpectedDef expectedDef : EXPECTED_DEFS) {
            AccessConstraintKey acdKey = expectedDef.key;
            String constraint = ModelDescriptionConstants.SENSITIVE.equals(acdKey.getType())
                    ? ModelDescriptionConstants.SENSITIVITY_CLASSIFICATION
                    : ModelDescriptionConstants.APPLICATION_CLASSIFICATION;
            String acdType = acdKey.isCore() ? "core" : acdKey.getSubsystemName();
            String path = String.format(ADDR_FORMAT, acdKey.getType(), acdType, acdKey.getName());
            ModelNode op = createOpNode(path, READ_CHILDREN_RESOURCES_OPERATION);
            op.get(ModelDescriptionConstants.CHILD_TYPE).set(ModelDescriptionConstants.APPLIES_TO);
            //System.out.println("Testing " + acdKey);
            ModelNode result = RbacUtil.executeOperation(client, op, Outcome.SUCCESS).get(ModelDescriptionConstants.RESULT);
            Assert.assertTrue(acdKey + "result is defined", result.isDefined());
            Assert.assertTrue(acdKey + "result has content", result.asInt() > 0);
            boolean foundResource = false;
            boolean foundAttr = false;
            boolean foundOps = false;
            for (Property prop : result.asPropertyList()) {
                ModelNode pathResult = prop.getValue();
                if (pathResult.get(ModelDescriptionConstants.ENTIRE_RESOURCE).asBoolean()) {
                    Assert.assertTrue(acdKey + " -- " + prop.getName() + " resource", expectedDef.expectResource);
                    foundResource = true;
                }
                ModelNode attrs = pathResult.get(ATTRIBUTES);
                if (attrs.isDefined() && attrs.asInt() > 0) {
                    Assert.assertTrue(acdKey + " -- " + prop.getName() + " attributes = " + attrs.asString(), expectedDef.expectAttributes);
                    foundAttr = true;
                }
                ModelNode ops = pathResult.get(OPERATIONS);
                if (ops.isDefined() && ops.asInt() > 0) {
                    Assert.assertTrue(acdKey + " -- " + prop.getName() + " operations = " + ops.asString(), expectedDef.expectOps);
                    foundOps = true;
                }
            }

            Assert.assertEquals(acdKey + " -- resource", expectedDef.expectResource, foundResource);
            Assert.assertEquals(acdKey + " -- attributes", expectedDef.expectAttributes, foundAttr);
            Assert.assertEquals(acdKey + " -- operations", expectedDef.expectOps, foundOps);
        }
    }

    private static AccessConstraintKey getAppKey(String subsystemName, String name) {
        return new AccessConstraintKey(ModelDescriptionConstants.APPLICATION_CLASSIFICATION, false, subsystemName, name);
    }

    private static AccessConstraintKey getSensKey(String subsystemName, String name) {
        return new AccessConstraintKey(ModelDescriptionConstants.SENSITIVITY_CLASSIFICATION, false, subsystemName, name);
    }
}
