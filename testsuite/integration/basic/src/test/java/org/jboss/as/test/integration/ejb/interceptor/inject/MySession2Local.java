/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

import jakarta.ejb.Local;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Local
public interface MySession2Local {
    boolean doit();
}
