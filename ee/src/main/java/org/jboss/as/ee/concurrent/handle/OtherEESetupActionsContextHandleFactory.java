/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent.handle;

import org.jboss.as.ee.EeLogger;
import org.jboss.as.ee.EeMessages;
import org.jboss.as.server.deployment.SetupAction;

import javax.enterprise.concurrent.ContextService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The context handle factory responsible for setting EE setup actions in the invocation context.
 *
 * @author Eduardo Martins
 */
public class OtherEESetupActionsContextHandleFactory implements ContextHandleFactory {

    public static final String NAME = "EE_SETUP_ACTIONS";

    private final List<SetupAction> setupActions;

    public OtherEESetupActionsContextHandleFactory(List<SetupAction> setupActions) {
        this.setupActions = setupActions;
    }

    @Override
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new OtherEESetupActionsContextHandle(setupActions);
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
    public void writeHandle(ContextHandle contextHandle, ObjectOutputStream out) throws IOException {
    }

    @Override
    public ContextHandle readHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return new OtherEESetupActionsContextHandle(setupActions);
    }

    private static class OtherEESetupActionsContextHandle implements ContextHandle {

        private final List<SetupAction> setupActions;
        private LinkedList<SetupAction> resetActions;

        private OtherEESetupActionsContextHandle(List<SetupAction> setupActions) {
            this.setupActions = setupActions;
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        @Override
        public void setup() throws IllegalStateException {
            resetActions = new LinkedList<>();
            try {
                for (SetupAction setupAction : this.setupActions) {
                    setupAction.setup(Collections.<String, Object>emptyMap());
                    resetActions.addFirst(setupAction);
                }
            } catch (Error | RuntimeException e) {
                reset();
                throw e;
            }
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
            throw EeMessages.MESSAGES.serializationMustBeHandledByThefactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw EeMessages.MESSAGES.serializationMustBeHandledByThefactory();
        }
    }
}
