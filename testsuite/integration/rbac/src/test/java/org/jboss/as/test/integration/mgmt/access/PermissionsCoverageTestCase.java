/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mgmt.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.management.rbac.PermissionsCoverageTestUtil.assertTheEntireDomainTreeHasPermissionsDefined;

import java.io.IOException;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
@RunWith(Arquillian.class)
public class PermissionsCoverageTestCase {
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
    public void testTheEntireDomainTreeHasPermissionsDefined() throws Exception {
        ModelControllerClient client = managementClient.getControllerClient();
        assertTheEntireDomainTreeHasPermissionsDefined(client);
    }
}
