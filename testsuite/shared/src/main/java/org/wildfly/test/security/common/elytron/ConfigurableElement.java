/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Interface representing a configurable object in domain model. The implementation has to override at least one of the
 * {@code create(...)} methods and one of the {@code remove(...)} methods.
 *
 * @author Josef Cacek
 */
public interface ConfigurableElement {

    /**
     * Returns name of this element.
     */
    String getName();

    /**
     * Creates this element in domain model and also creates other resources if needed (e.g. external files)
     *
     * @param cli connected {@link CLIWrapper} instance
     */
    default void create(CLIWrapper cli) throws Exception {
        throw new IllegalStateException("The create() method was not properly implemented");
    }

    /**
     * Creates this element in domain model and it also may create other resources if needed (e.g. external files).
     * Implementation can choose if controller client is used or provided CLI wrapper.
     */
    default void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        create(cli);
    }

    /**
     * Reverts the changes introdued by {@code create(...)} method(s).
     */
    default void remove(CLIWrapper cli) throws Exception {
        throw new IllegalStateException("The remove() method was not properly implemented");
    }

    /**
     * Reverts the changes introdued by {@code create(...)} method(s).
     */
    default void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        remove(cli);
    }
}
