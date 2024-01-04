/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

/**
 * Represents common piece in CLI commands, which can be shared across types.
 *
 * @author Josef Cacek
 */
public interface CliFragment {

    /**
     * Generates part of CLI string which uses configuration for this fragment.
     *
     * @return part of CLI command
     */
    String asString();

}