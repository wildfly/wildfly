/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remove.method;

import jakarta.ejb.Remote;

/**
 * A Delegate to invoke upon local views of the Remove EJBs
 *
 * @author <a href="arubinge@redhat.com">ALR</a>
 */
@Remote
public interface Delegate {
    String invokeStatelessRemove();

    String invokeStatefulRemove();
}
