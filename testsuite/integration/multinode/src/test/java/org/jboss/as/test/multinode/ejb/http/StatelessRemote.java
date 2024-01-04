/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.ejb.http;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface StatelessRemote extends jakarta.ejb.EJBObject {
    int remoteCall() throws Exception;
    int method() throws Exception;
}
