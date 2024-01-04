/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

/**
 * Type provider spi.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface TypeProvider {
    /**
     * Try getting type off config.
     *
     * @param visitor the current config visitor
     * @param previous previous config visitor node
     * @return type
     */
    Class<?> getType(ConfigVisitor visitor, ConfigVisitorNode previous);
}
