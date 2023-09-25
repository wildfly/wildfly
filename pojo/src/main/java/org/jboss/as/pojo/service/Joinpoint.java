/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

/**
 * Invoke a method or ctor.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
interface Joinpoint {
    /**
     * Dispatch this action.
     *
     * @return dispatch result
     * @throws Throwable for any error
     */
    Object dispatch() throws Throwable;
}
