/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
