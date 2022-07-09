/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.multinode.remotecall.scoped.context;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Setup for passivation test case.
 *
 * @author Jaikiran Pai
 * @author Richard Achmatowicz
 */
public class PassivationConfigurationSetup implements ServerSetupTask {
    private static final Logger log = Logger.getLogger(PassivationConfigurationSetup.class);

    /**
     * This test setup originally depended upon manipulating the passivation-store for the default (passivating) cache.
     * However, since WFLY-14953, passivation stores have been superceeded by bean-management-providers
     * (i.e. use /subsystem=distributable-ejb/infinispan-bean-management=default instead of /subsystem=ejb3/passivation-store=infinispan)
     */
    private static final PathAddress INFINISPAN_BEAN_MANAGEMENT_PATH = PathAddress.pathAddress(PathElement.pathElement("subsystem", "distributable-ejb"),
            PathElement.pathElement("infinispan-bean-management", "default"));

    /*
     * Set the max-active-beans attribute of the bean-management provider to 1 to force passivation.
     */
    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        ModelNode operation = Util.getWriteAttributeOperation(INFINISPAN_BEAN_MANAGEMENT_PATH, "max-active-beans", 1);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        log.trace("modelnode operation write-attribute max-active-beans=1: " + result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        ServerReload.reloadIfRequired(managementClient);
    }

    /*
     * Return max-active-beans to its configured value (10,000).
     * NOTE: the configured default is 10000 but may change over time.
     */
    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        ModelNode operation = Util.getWriteAttributeOperation(INFINISPAN_BEAN_MANAGEMENT_PATH, "max-active-beans", 10000);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        ServerReload.reloadIfRequired(managementClient);
    }
}