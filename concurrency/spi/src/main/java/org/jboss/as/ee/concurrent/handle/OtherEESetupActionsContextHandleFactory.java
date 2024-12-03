/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.server.deployment.SetupAction;

/**
 * The context handle factory responsible for setting EE setup actions in the invocation context.
 *
 * @author Eduardo Martins
 */
public class OtherEESetupActionsContextHandleFactory implements EE10ContextHandleFactory {

    public static final String NAME = "EE_SETUP_ACTIONS";

    private final List<SetupAction> setupActions;
    private final SetupContextHandle clearedContextHandle;

    public OtherEESetupActionsContextHandleFactory(List<SetupAction> setupActions) {
        this.setupActions = setupActions;
        this.clearedContextHandle = new ClearedSetupContextHandle(setupActions);
    }

    @Override
    public String getContextType() {
        return ContextServiceDefinition.APPLICATION;
    }

    @Override
    public SetupContextHandle clearedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return clearedContextHandle;
    }

    @Override
    public SetupContextHandle propagatedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new PropagatedSetupContextHandle(setupActions);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getChainPriority() {
        return 400;
    }

    @Override
    public void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException {
        out.writeBoolean(contextHandle != clearedContextHandle);
    }

    @Override
    public SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return in.readBoolean() ? new PropagatedSetupContextHandle(setupActions) : clearedContextHandle;
    }

    private static class PropagatedSetupContextHandle implements SetupContextHandle {

        private static final long serialVersionUID = 5698880356954066079L;
        private final List<SetupAction> setupActions;

        private PropagatedSetupContextHandle(List<SetupAction> setupActions) {
            this.setupActions = setupActions;
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        @Override
        public org.jboss.as.ee.concurrent.handle.ResetContextHandle setup() throws IllegalStateException {
            final LinkedList<SetupAction> resetActions = new LinkedList<>();
            final PropagatedResetContextHandle resetContextHandle = new PropagatedResetContextHandle(resetActions);
            try {
                for (SetupAction setupAction : this.setupActions) {
                    setupAction.setup(Collections.<String, Object>emptyMap());
                    resetActions.addFirst(setupAction);
                }
            } catch (Error | RuntimeException e) {
                resetContextHandle.reset();
                throw e;
            }
            return resetContextHandle;
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }

    private static class PropagatedResetContextHandle implements ResetContextHandle {

        private static final long serialVersionUID = -1279030727101664631L;
        private List<SetupAction> resetActions;

        private PropagatedResetContextHandle(List<SetupAction> resetActions) {
            this.resetActions = resetActions;
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        @Override
        public void reset() {
            if(resetActions != null) {
                for (SetupAction resetAction : this.resetActions) {
                    try {
                        resetAction.teardown(Collections.<String, Object>emptyMap());
                    } catch (Throwable e) {
                        EeLogger.ROOT_LOGGER.debug("failed to teardown action",e);
                    }
                }
                resetActions = null;
            }
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }

    private static class ClearedSetupContextHandle implements SetupContextHandle {

        private static final long serialVersionUID = 175538978733960332L;
        private final List<SetupAction> setupActions;

        private ClearedSetupContextHandle(List<SetupAction> setupActions) {
            this.setupActions = setupActions;
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        @Override
        public org.jboss.as.ee.concurrent.handle.ResetContextHandle setup() throws IllegalStateException {
            // we probably should instead have a thread stack with the current setup actions and restore current on reset?
            new PropagatedResetContextHandle(setupActions).reset();
            return new ResetContextHandle() {
                @Override
                public void reset() {
                }
                @Override
                public String getFactoryName() {
                    return NAME;
                }
            };
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }

}
