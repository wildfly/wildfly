/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import org.jboss.as.ee.logging.EeLogger;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The configuration for a Context Service, which indicates if a context type should be cleared, propagated or unchanged.
 * @author emmartins
 */
public class ContextServiceTypesConfiguration implements Serializable  {

    public static final ContextServiceTypesConfiguration DEFAULT = new ContextServiceTypesConfiguration(null, null, null);

    private static final long serialVersionUID = -8818025042707301480L;

    private final Set<String> cleared;
    private final Set<String> propagated;
    private final Set<String> unchanged;

    /**
     *
     * @param cleared
     * @param propagated
     * @param unchanged
     */
    private ContextServiceTypesConfiguration(Set<String> cleared, Set<String> propagated, Set<String> unchanged) {
        if (cleared == null || cleared.isEmpty()) {
            // spec default for cleared includes only Transaction
            this.cleared = Set.of(ContextServiceDefinition.TRANSACTION);
        } else {
            this.cleared = Collections.unmodifiableSet(cleared);
        }
        if (propagated == null || propagated.isEmpty()) {
            // spec default for propagation includes all remaining, i.e. not in "cleared" and not in "unchanged"
            this.propagated = Set.of(ContextServiceDefinition.ALL_REMAINING);
        } else {
            this.propagated = Collections.unmodifiableSet(propagated);
        }
        if (unchanged == null || unchanged.isEmpty()) {
            // spec default for unchanged includes none, which is represented by a single empty element
            this.unchanged = Set.of("");
        } else {
            this.unchanged = Collections.unmodifiableSet(unchanged);
        }
    }

    /**
     *
     * @param contextType
     * @return true if the specified contextType should be cleared, false otherwise
     */
    public boolean isCleared(String contextType) {
        return isTypeIncluded(contextType, cleared, propagated, unchanged);
    }

    /**
     *
     * @param contextType
     * @return true if the specified contextType should be propagated, false otherwise
     */
    public boolean isPropagated(String contextType) {
        return isTypeIncluded(contextType, propagated, cleared, unchanged);
    }

    /**
     *
     * @param contextType
     * @return true if the specified contextType should be unchanged, false otherwise
     */
    public boolean isUnchanged(String contextType) {
        return isTypeIncluded(contextType, unchanged, cleared, propagated);
    }

    /**
     * Checks if a contextType is included in a set of contextTypes.
     * @param contextType
     * @param contextTypes
     * @param otherContextTypes1
     * @param otherContextTypes2
     * @return true if contextType is in contextTypes, or if contextTypes contains ContextServiceDefinition.ALL_REMAINING and contextType not in otherContextTypes1 and contextType not in otherContextTypes2; false otherwise
     */
    private boolean isTypeIncluded(String contextType, Set<String> contextTypes, Set<String> otherContextTypes1, Set<String> otherContextTypes2) {
        Objects.requireNonNull(contextType);
        if (contextTypes.contains(contextType)) {
            return true;
        }
        if (contextTypes.contains(ContextServiceDefinition.ALL_REMAINING)) {
            return !otherContextTypes1.contains(contextType) && !otherContextTypes2.contains(contextType);
        }
        return false;
    }

    /**
     * The builder class.
     */
    public static class Builder {
        private Set<String> cleared;
        private Set<String> propagated;
        private Set<String> unchanged;

        /**
         *
         * @param cleared
         * @return
         */
        public Builder setCleared(Set<String> cleared) {
            this.cleared = cleared;
            return this;
        }

        /**
         *
         * @param cleared
         * @return
         */
        public Builder setCleared(String[] cleared) {
            if (cleared == null || cleared.length == 0) {
                this.cleared = null;
            } else {
                this.cleared = new HashSet<>();
                Collections.addAll(this.cleared, cleared);
            }
            return this;
        }

        /**
         *
         * @param propagated
         * @return
         */
        public Builder setPropagated(Set<String> propagated) {
            this.propagated = propagated;
            return this;
        }

        /**
         *
         * @param propagated
         * @return
         */
        public Builder setPropagated(String[] propagated) {
            if (propagated == null || propagated.length == 0) {
                this.propagated = null;
            } else {
                this.propagated = new HashSet<>();
                Collections.addAll(this.propagated, propagated);
            }
            return this;
        }

        /**
         *
         * @param unchanged
         * @return
         */
        public Builder setUnchanged(Set<String> unchanged) {
            this.unchanged = unchanged;
            return this;
        }

        /**
         *
         * @param unchanged
         * @return
         */
        public Builder setUnchanged(String[] unchanged) {
            if (unchanged == null || unchanged.length == 0) {
                this.unchanged = null;
            } else {
                this.unchanged = new HashSet<>();
                Collections.addAll(this.unchanged, unchanged);
            }
            return this;
        }

        /**
         * @return a new ContextServiceTypesConfiguration instance with the set values of cleared, propagated and unchanged
         * @throws IllegalStateException if there are multiple usages of ContextServiceDefinition.ALL_REMAINING currently set
         */
        public ContextServiceTypesConfiguration build() throws IllegalStateException {
            // validate there are not multiple ContextServiceDefinition.ALL_REMAINING
            int remainingCount = 0;
            if (cleared != null && cleared.contains(ContextServiceDefinition.ALL_REMAINING)) {
                remainingCount++;
            }
            if (propagated == null || propagated.isEmpty() || propagated.contains(ContextServiceDefinition.ALL_REMAINING)) {
                remainingCount++;
            }
            if (unchanged != null && unchanged.contains(ContextServiceDefinition.ALL_REMAINING)) {
                remainingCount++;
            }
            if (remainingCount > 1) {
                throw EeLogger.ROOT_LOGGER.multipleUsesOfAllRemaining();
            }
            if ((cleared == null || cleared.isEmpty()) && (propagated == null || propagated.isEmpty()) && (unchanged == null || unchanged.isEmpty())) {
                return DEFAULT;
            } else {
                return new ContextServiceTypesConfiguration(cleared, propagated, unchanged);
            }
        }
    }
}
