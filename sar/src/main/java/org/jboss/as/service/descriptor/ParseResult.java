/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service.descriptor;

/**
 * The result of a parsing operation.
 *
 * @param <T> the parse result type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ParseResult<T> {

    private T result;

    /**
     * Set the result.
     *
     * @param result the parsing result
     */
    public void setResult(T result) {
        this.result = result;
    }

    /**
     * Get the result.
     *
     * @return the parsing result
     */
    public T getResult() {
        return result;
    }
}
