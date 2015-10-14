/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.secman.subsystem;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.logging.Logger;

/**
 *
 * @author Josef Cacek
 */
public abstract class ReloadableCliTestBase extends AbstractCliTestBase {

    private static Logger LOGGER = Logger.getLogger(ReloadableCliTestBase.class);

    @ArquillianResource
    private ManagementClient managementClient;

    protected void doCliOperation(final String operation) throws Exception {
        LOGGER.debugv("Performing CLI operation: {0}", operation);
        initCLI();
        cli.sendLine(operation);
    }

    protected void doCliOperationWithoutChecks(final String operation) throws Exception {
        LOGGER.debugv("Performing CLI operation without checks: {0}", operation);
        initCLI();
        cli.sendLine(operation, true);
        final CLIOpResult result = cli.readAllAsOpResult();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Operation result is: {0}", result.getResponseNode());
        }
    }

    protected void reloadServer() {
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

}
