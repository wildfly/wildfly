/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
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
