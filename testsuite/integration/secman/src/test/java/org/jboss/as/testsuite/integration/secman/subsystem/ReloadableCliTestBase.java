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

import static org.junit.Assert.assertTrue;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Abstract parent for tests which needs to do CLI operations and reload the server.
 *
 * @author Josef Cacek
 */
public abstract class ReloadableCliTestBase extends AbstractCliTestBase {

    private static Logger LOGGER = Logger.getLogger(ReloadableCliTestBase.class);

    @ArquillianResource
    private ManagementClient managementClient;

    /**
     * Executes given CLI operation and returns the result. It throws {@link AssertionError} if the operation fails.
     *
     * @param operation CLI operation
     * @return operation result
     * @throws Exception initialization of the CLI or results reading fails
     */
    protected CLIOpResult doCliOperation(final String operation) throws Exception {
        LOGGER.debugv("Performing CLI operation: {0}", operation);
        initCLI();
        cli.sendLine(operation);
        return cli.readAllAsOpResult();
    }

    /**
     * Executes given CLI operation and returns the result. It doesn't check if the operation finished without errors.
     *
     * @param operation CLI operation
     * @return operation result
     * @throws Exception initialization of the CLI or results reading fails
     */
    protected CLIOpResult doCliOperationWithoutChecks(final String operation) throws Exception {
        LOGGER.debugv("Performing CLI operation without checks: {0}", operation);
        initCLI();
        cli.sendLine(operation, true);
        final CLIOpResult result = cli.readAllAsOpResult();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Operation result is: {0}", result.getResponseNode());
        }
        return result;
    }

    /**
     * Executes server reload command and waits for completion.
     */
    protected void reloadServer() {
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    /**
     * Asserts that given operation result contains requirement for server reload.
     *
     * @param opResult
     */
    protected void assertOperationRequiresReload(CLIOpResult opResult) {
        final ModelNode responseNode = opResult.getResponseNode();
        final String[] names = new String[] { "response-headers", "operation-requires-reload" };
        assertTrue("Operation should require reload",
                responseNode != null && responseNode.hasDefined(names) && responseNode.get(names).asBoolean());
    }

}
