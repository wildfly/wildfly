/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.stateful.passivation;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * Setup for {@link EagerPassivationTestCase}.
 * @author Paul Ferraro
 */
public class EagerPassivationTestCaseSetup implements ServerSetupTask {

    private static final String EAGER_PASSIVATION_PROPERTY = "jboss.ejb.stateful.idle-timeout";

    @Override
    public void setup(ManagementClient client, String containerId) throws Exception {
        ModelNode operation = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement("system-property", EAGER_PASSIVATION_PROPERTY)));
        operation.get(ModelDescriptionConstants.VALUE).set("PT1S");
        ModelNode result = client.getControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        ServerReload.reloadIfRequired(client);
    }

    @Override
    public void tearDown(ManagementClient client, String containerId) throws Exception {
        ModelNode operation = Util.createRemoveOperation(PathAddress.pathAddress(PathElement.pathElement("system-property", EAGER_PASSIVATION_PROPERTY)));
        ModelNode result = client.getControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        ServerReload.reloadIfRequired(client);
    }
}