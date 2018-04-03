/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.security.loginmodules.policy;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.security.common.AbstractTraceLoggingServerSetupTask;
import org.jboss.dmr.ModelNode;

import java.util.Arrays;
import java.util.Collection;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

/**
 * Server setup class for PolicyRegistrationTestCase. Sets TRACE logging for the logger "org.jboss.security".
 *
 * @author Daniel Cihak
 */
public class SecurityTraceServerSetupTask extends AbstractTraceLoggingServerSetupTask {

    public static String SERVER_LOG_DIR_VALUE;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        super.setup(managementClient, containerId);

        ModelNode getLogDir = new ModelNode();
        getLogDir.get(OP).set("resolve-expression");
        getLogDir.get("expression").set("${jboss.server.log.dir}");
        SERVER_LOG_DIR_VALUE = ManagementOperations.executeOperation(managementClient.getControllerClient(), getLogDir).asString();
    }

    protected Collection<String> getCategories(ManagementClient managementClient, String containerId) {
        return Arrays.asList("org.jboss.security");
    }
}
