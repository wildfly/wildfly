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

package org.jboss.as.domain.management.security.state;

import java.util.Collection;
import java.util.Iterator;

/**
 * Where multiple stages of validation need to be performed this {@link State} provides a way to coordinate ensuring each stage
 * of the validation is performed.
 *
 * One instance of this state should be created when the validation begins and as each step passes this instance should be used
 * as the next state. Should validation fail then the instance of this State should be discarded so that validation can commence
 * from the beginning if alternative values are provided.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class AbstractValidationState implements State {

    private Iterator<State> stateIterator;

    @Override
    public State execute() {
        if (stateIterator == null) {
            stateIterator = getValidationStates().iterator();
        }

        if (stateIterator.hasNext()) {
            return stateIterator.next();
        }

        return getSuccessState();
    }

    /**
     * Get a {@link Collection} containing all states required to perform the validation needed.
     *
     * On initialisation an Iterator will be created for this collection - each time this state is called the next state
     * returned by the Iterator will be called to perform validation. Once the Iterator is exhausted the success state will be
     * returned instead.
     *
     * If validation fails then it is expected that the individual validation states will transition away from this state, this
     * is why there is no error or failure state.
     */
    protected abstract Collection<State> getValidationStates();

    /**
     * Get the state to transition to once all validation is complete.
     *
     * @return The state to transition to once all validation is complete.
     */
    protected abstract State getSuccessState();

}
