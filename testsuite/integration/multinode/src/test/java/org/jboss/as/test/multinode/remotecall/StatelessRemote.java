/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.remotecall;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface StatelessRemote extends jakarta.ejb.EJBObject {
    void localCall() throws Exception;

    void localHomeCall() throws Exception;

    int remoteCall() throws Exception;

    int remoteHomeCall() throws Exception;

    int method() throws Exception;

    int homeMethod() throws Exception;
}
