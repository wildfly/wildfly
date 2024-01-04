/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.api;

/**
 * Simple bean factory interface.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface BeanFactory {
    /**
     * Create new bean.
     *
     * @return new bean
     * @throws Throwable for any error
     */
    Object create() throws Throwable;
}
