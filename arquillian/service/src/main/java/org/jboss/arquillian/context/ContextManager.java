/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.arquillian.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Sets up and tears down a set of contexts, represented by a list of {@link SetupAction}s. If {@link #setup()} completes
 * successfully then {@link #teardown()} must be called.
 *
 * @author Stuart Douglas
 *
 */
public class ContextManager {

    private final List<SetupAction> setupActions;

    ContextManager(List<SetupAction> setupActions) {
        this.setupActions = Collections.unmodifiableList(new ArrayList<SetupAction>(setupActions));
    }

    /**
     * Sets up the contexts. If any of the setup actions fail then any setup contexts are torn down, and then the exception is
     * wrapped and thrown
     */
    public void setup() {
        final List<SetupAction> sucessfulActions = new ArrayList<SetupAction>();
        for (SetupAction action : setupActions) {
            try {
                action.setup();
                sucessfulActions.add(action);
            } catch (Throwable e) {
                for (SetupAction s : sucessfulActions) {
                    try {
                        s.teardown();
                    } catch (Throwable t) {
                        // we ignore these, and just propegate the exception that caused the setup to fail
                    }
                }
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Tears down the contexts. If an exception is thrown by a {@link SetupAction} it is wrapped and re-thrown after all
     * {@link SetupAction#teardown()} methods have been called.
     * <p>
     * Contexts are torn down in the oposite order to which they are set up (i.e. the first context set up is the last to be
     * torn down).
     * <p>
     * If more than one teardown() method thrown an exception then only the first is propegated.
     */
    public void teardown() {
        Throwable exceptionToThrow = null;
        ListIterator<SetupAction> itr = setupActions.listIterator(setupActions.size());
        while (itr.hasPrevious()) {
            SetupAction action = itr.previous();
            try {
                action.setup();
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
