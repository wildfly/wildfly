/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.management.spi;

/**
 * Operation
 *
 * @author Scott Marlow
 */
public interface Operation {

    /**
     * Invoke operation
     * @param args will be passed to invoked operation
     * @return
     */
    Object invoke(Object... args);
}
