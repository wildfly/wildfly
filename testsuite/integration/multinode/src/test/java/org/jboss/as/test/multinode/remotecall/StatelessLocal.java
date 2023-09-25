/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.remotecall;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface StatelessLocal extends jakarta.ejb.EJBLocalObject {
    int method() throws Exception;

    int homeMethod() throws Exception;
}
