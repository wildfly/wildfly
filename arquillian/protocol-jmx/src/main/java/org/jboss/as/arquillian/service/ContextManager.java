/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.jboss.as.server.deployment.SetupAction;

/**
 * Sets up and tears down a set of contexts, represented by a list of {@link SetupAction}s. If {@link #setup(java.util.Map)} completes
 * successfully then {@link #teardown(java.util.Map)} must be called.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 *
 */
public class ContextManager {

    private final List<SetupAction> setupActions;

    ContextManager(final List<SetupAction> setupActions) {
        final List<SetupAction> actions = new ArrayList<SetupAction>(setupActions);
        Collections.sort(actions, new Comparator<SetupAction>() {

            @Override
            public int compare(final SetupAction arg0, final SetupAction arg1) {
                return arg0.priority() > arg1.priority() ? -1 : arg0.priority() == arg1.priority() ? 0 : 1;
            }
        });
        this.setupActions = Collections.unmodifiableList(actions);
    }

    /**
     * Sets up the contexts. If any of the setup actions fail then any setup contexts are torn down, and then the exception is
     * wrapped and thrown
     */
    public void setup(final Map<String, Object> properties) {
        final List<SetupAction> successfulActions = new ArrayList<SetupAction>();
        for (final SetupAction action : setupActions) {
            try {
                action.setup(properties);
                successfulActions.add(action);
            } catch (final Throwable e) {
                for (SetupAction s : successfulActions) {
                    try {
                        s.teardown(properties);
                    } catch (final Throwable t) {
                        // we ignore these, and just propagate the exception that caused the setup to fail
                    }
                }
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Tears down the contexts. If an exception is thrown by a {@link SetupAction} it is wrapped and re-thrown after all
     * {@link SetupAction#teardown(java.util.Map)} methods have been called.
     * <p>
     * Contexts are torn down in the opposite order to which they are set up (i.e. the first context set up is the last to be
     * torn down).
     * <p>
     * If more than one teardown() method thrown an exception then only the first is propagated.
     */
    public void teardown(final Map<String, Object> properties) {
        Throwable exceptionToThrow = null;
        final ListIterator<SetupAction> itr = setupActions.listIterator(setupActions.size());
        while (itr.hasPrevious()) {
            final SetupAction action = itr.previous();
            try {
                action.teardown(properties);
            } catch (Throwable e) {
                if (exceptionToThrow == null) {
                    exceptionToThrow = e;
                }
            }
        }
        if (exceptionToThrow != null) {
            throw new RuntimeException(exceptionToThrow);
        }
    }
}
