/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.txn.subsystem;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;

/**
 * Handler for explicitly running Narayana recovery scan.
 */
public class LogStoreProcessRecoveryHandler implements OperationStepHandler {
    static final LogStoreProcessRecoveryHandler INSTANCE = new LogStoreProcessRecoveryHandler();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        RecoveryManager.manager().scan();
        // necessary to process two sequential scans to be sure that the whole recovery processing was run
        // if a recovery processing is in progress during the first scan then the first execution
        // only waits for the currently processing to finish and would not go through both phases
        RecoveryManager.manager().scan();
    }

}
