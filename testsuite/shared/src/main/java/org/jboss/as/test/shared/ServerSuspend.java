/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.shared;

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ServerSuspend {
    private static final ModelNode READ_SUSPEND_STATE_OP = Operations.createReadAttributeOperation(new ModelNode().setEmptyList(), "suspend-state");

    /**
     * Suspends the server with the default timeout.
     *
     * @param client the client used to execute the operation
     *
     * @throws IOException if an error occurs with client communications
     */
    public static void suspend(final ModelControllerClient client) throws IOException {
        final ModelNode result = client.execute(Operations.createOperation("suspend"));
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to suspend server: " + Operations.getFailureDescription(result).asString());
        }
    }

    /**
     * Suspends the server with the timeout.
     *
     * @param client  the client used to execute the operation
     * @param timeout the timeout, in seconds, the suspend operation will wait
     *
     * @throws IOException if an error occurs with client communications
     */
    public static void suspend(final ModelControllerClient client, final int timeout) throws IOException {
        final ModelNode op = Operations.createOperation("suspend");
        op.get("timeout").set(timeout);
        final ModelNode result = client.execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to suspend server: " + Operations.getFailureDescription(result).asString());
        }
    }

    /**
     * Resumes a suspended server.
     *
     * @param client the client used to execute the operation
     *
     * @throws IOException if an error occurs with client communications
     */
    public static void resume(final ModelControllerClient client) throws IOException {
        final ModelNode result = client.execute(Operations.createOperation("resume"));
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to resume server: " + Operations.getFailureDescription(result).asString());
        }
    }

    /**
     * Checks to see if the server is in a suspended state.
     *
     * @param client the client used to execute the operation
     *
     * @return {@code true} if the server is in a suspended state, otherwise {@code false}
     *
     * @throws IOException if an error occurs with client communications
     */
    public static boolean isSuspended(final ModelControllerClient client) throws IOException {
        return isSuspendState(client, "SUSPENDED");
    }

    /**
     * Checks to see if the server is in a resumed, {@code RUNNING}, state.
     *
     * @param client the client used to execute the operation
     *
     * @return {@code true} if the server is in a resumed state, otherwise {@code false}
     *
     * @throws IOException if an error occurs with client communications
     */
    public static boolean isResumed(final ModelControllerClient client) throws IOException {
        return isSuspendState(client, "RUNNING");
    }

    /**
     * Reads the current value of the {@code suspend-state} attribute.
     *
     * @param client the client used to execute the operation
     *
     * @return the value of the {@code suspend-state} attribute
     *
     * @throws IOException if an error occurs with client communications
     */
    public static String getSuspendState(final ModelControllerClient client) throws IOException {
        final ModelNode result = client.execute(READ_SUSPEND_STATE_OP);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed read suspend state: " + Operations.getFailureDescription(result).asString());
        }
        return Operations.readResult(result).asString();
    }

    private static boolean isSuspendState(final ModelControllerClient client, final String state) throws IOException {
        return getSuspendState(client).equalsIgnoreCase(state);
    }
}
