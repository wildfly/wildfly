/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.remote.ejb;

/**
 * @author James Livingston
 */
@jakarta.ejb.Remote
public interface BinderRemote extends Remote {
    void bind();
    void rebind();
    void unbind();
}
