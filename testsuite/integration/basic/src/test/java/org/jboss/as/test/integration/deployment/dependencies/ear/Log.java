/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.dependencies.ear;

/**
 * A Log. Simple logger - only holds static {@link StringBuffer} instance.
 *
 * @author Josef Cacek
 */
public class Log {

    /** The log. */
    public static final StringBuffer SB = new StringBuffer();

}
