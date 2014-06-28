/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.core.test.standalone.base;

import javax.inject.Inject;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.wildfly.core.testrunner.ManagementClient;

/**
 * Class that is extended by management tests that can use resource injection to get the management client
 *
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public abstract class ContainerResourceMgmtTestBase extends AbstractMgmtTestBase {

    @Inject
    private static ManagementClient managementClient;


    public ManagementClient getManagementClient() {
        return managementClient;
    }


    public void setManagementClient(ManagementClient managementClient) {
        this.managementClient = managementClient;
    }


    @Override
    protected ModelControllerClient getModelControllerClient() {
        return managementClient.getControllerClient();
    }
}

