/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.transactions;

import com.arjuna.ats.arjuna.recovery.RecoveryDriver;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * A helper class which wraps remote execution of the transaction recovery.
 */
public class RecoveryExecutor {
    private static final Logger log = Logger.getLogger(RecoveryExecutor.class);

    private static final ModelNode ADDRESS_TRANSACTIONS
            = new ModelNode().add("subsystem", "transactions");
    private static final ModelNode ADDRESS_SOCKET_BINDING
            = new ModelNode().add(ClientConstants.SOCKET_BINDING_GROUP, "standard-sockets");
    private static final ModelNode ADDRESS_TRANSACTIONS_LOG_STORE = ADDRESS_TRANSACTIONS
            .clone().add("log-store", "log-store");
    static {
        ADDRESS_TRANSACTIONS.protect();
        ADDRESS_TRANSACTIONS.protect();
        ADDRESS_SOCKET_BINDING.protect();
    }

    private static final int DEFAULT_SOCKET_READ_SCAN_TIMEOUT_MS = 60 * 1000;
    private static final int RECOVERY_SCAN_RETRY_COUNT = 5;

    private final ManagementClient managementClient;
    private final AtomicReference<RecoveryDriver> recoveryDriverReference = new AtomicReference<>();

    public RecoveryExecutor(ManagementClient managementClient) {
        this.managementClient = managementClient;
    }

    /**
     * Run transaction recovery with default read socket timeout.
     * See more at {@link #runTransactionRecovery(int)}.
     *
     * @return true if recovery was successfully run without any issue, false otherwise
     */
    public boolean runTransactionRecovery() {
        return runTransactionRecovery(DEFAULT_SOCKET_READ_SCAN_TIMEOUT_MS);
    }

    /**
     * <p>
     * Run the transaction recovery. It expects the <code>transaction-listener</code> is enabled
     * (<code>&sol;subsystem=transactions&sol;:write-attribute(name=transaction-listener, value=true)</code>).
     * </p>
     * <p>
     * Returning from this method could not necessary means that whole recovery cycle was fully finished.
     * To be sure that the whole recovery cycle is finished is recommended to run this method twice (one by one).
     * After the second call it's ensured that one(!) recovery cycle is fully finished.
     * </p>
     * <p>
     * The method returns true if the recovery socket listener call succeeded and when the recovery cycle was run
     * without any issue. An issue means there is an error on recovery processing of an XAResource -
     * e.g. one example of such failure could be a database is down and recovery was not able to connect.
     * </p>
     *
     * @param socketReadTimeout  socket timeout for launching the recovery against the transaction listener socket,
     *                           expected to be opened already
     * @return true if recovery was successfully run without any issue, false otherwise
     */
    public boolean runTransactionRecovery(int socketReadTimeout) {
        try {
            return getRecoveryDriver().synchronousVerboseScan(TimeoutUtil.adjust(socketReadTimeout), RECOVERY_SCAN_RETRY_COUNT);
        } catch (Exception e) {
            throw new IllegalStateException("Error when triggering transaction recovery synchronous scan with RecoveryDriver "
                    + recoveryDriverReference.get() + ", based on the management client " + managementClient, e);
        }
    }

    /**
     * Running WildFly CLI operations to run ':recover' operations on all in-doubt transactions.
     */
    public void cliRecoverAllTransactions() {
        executeOperation(managementClient, ADDRESS_TRANSACTIONS_LOG_STORE, "probe");
        ModelNode logStoreModelNode = null;
        try {
            logStoreModelNode = readResource(managementClient, ADDRESS_TRANSACTIONS_LOG_STORE);
        } catch (MgmtOperationException | IOException e) {
            throw new IllegalStateException("Cannot read content of the transaction log store at " + ADDRESS_TRANSACTIONS_LOG_STORE
                + " with the management client" + managementClient, e);
        }
        for (ModelNode txns: logStoreModelNode.get("transactions").asList()) {
            String txnName = txns.asProperty().getName();
            for(ModelNode participants: txns.get(txnName).get("participants").asList()) {
                String participantName = participants.asProperty().getName();
                ModelNode participantAddress = ADDRESS_TRANSACTIONS_LOG_STORE.clone()
                        .add("transactions", txnName)
                        .add("participants", participantName);
                executeOperation(managementClient, participantAddress, "recover");
            }
        }
    }

    private RecoveryDriver getRecoveryDriver() {
        if(recoveryDriverReference.get() != null) return recoveryDriverReference.get();

        try {
            String transactionSocketBinding = readAttribute(managementClient, ADDRESS_TRANSACTIONS, "socket-binding").asString();
            final ModelNode addressSocketBinding = ADDRESS_SOCKET_BINDING.clone();
            addressSocketBinding.add(ClientConstants.SOCKET_BINDING, transactionSocketBinding);
            String host = readAttribute(managementClient, addressSocketBinding, "bound-address").asString();
            int port = readAttribute(managementClient, addressSocketBinding, "bound-port").asInt();
            recoveryDriverReference.compareAndSet(null, new RecoveryDriver(port, host));
            return recoveryDriverReference.get();
        } catch (MgmtOperationException | IOException e) {
            throw new IllegalStateException("Cannot obtain host:port for transaction recovery listener regarding" +
                    " the management client "  + managementClient);
        }
    }

    private ModelNode readAttribute(final ManagementClient managementClient, ModelNode address, String name) throws IOException, MgmtOperationException {
        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(ClientConstants.READ_ATTRIBUTE_OPERATION);
        operation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).set("true");
        operation.get(ModelDescriptionConstants.RESOLVE_EXPRESSIONS).set("true");
        operation.get(ClientConstants.NAME).set(name);
        return ManagementOperations.executeOperation(managementClient.getControllerClient(), operation);
    }

    private ModelNode readResource(final ManagementClient managementClient, ModelNode address) throws IOException, MgmtOperationException {
        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(ClientConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).set("true");
        operation.get(ModelDescriptionConstants.RESOLVE_EXPRESSIONS).set("true");
        operation.get(ModelDescriptionConstants.RECURSIVE).set("true");
        operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set("true");
        return ManagementOperations.executeOperation(managementClient.getControllerClient(), operation);
    }

    private void executeOperation(final ManagementClient managementClient, ModelNode address, String operationName) {
        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(operationName);
        try {
            // on the execution "outcome" different from the "success" the mgmt exception is thrown
            ManagementOperations.executeOperation(managementClient.getControllerClient(), operation);
        } catch (MgmtOperationException | IOException e) {
            throw new IllegalStateException("Cannot probe transaction subsystem log store at" + ADDRESS_TRANSACTIONS_LOG_STORE +
                    " via the management client " + managementClient, e);
        }
    }
}
