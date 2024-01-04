/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;

import javax.naming.Name;
import javax.naming.NamingException;

/**
 * Indicates that a naming store encountered a reference or a link when
 * performing a list operation. This saves the store from having to know
 * how to resolve the reference/link.
 *
 * @author Jason T. Greene
 */
public class RequireResolveException extends NamingException {
    private Name resolve;

    public RequireResolveException(Name resolve) {
        this.resolve = resolve;
    }

    public Name getResolve() {
        return resolve;
    }
}
