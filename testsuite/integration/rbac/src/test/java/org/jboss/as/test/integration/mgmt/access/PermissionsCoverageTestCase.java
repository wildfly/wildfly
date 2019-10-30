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
