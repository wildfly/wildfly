/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remove.method;

/**
 * AbstractRemoveBean.
 *
 * @author <a href="arubinge@redhat.com">ALR</a>
 */
public class AbstractRemoveBean implements Remove {
    // Class Members
    public static final String RETURN_STRING = "Remove";

    // Required Implementations
    public String remove() {
        return AbstractRemoveBean.RETURN_STRING;
    }
}
