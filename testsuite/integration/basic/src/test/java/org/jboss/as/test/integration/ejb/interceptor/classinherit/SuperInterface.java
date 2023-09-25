/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.classinherit;

import jakarta.ejb.Remote;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Remote
public interface SuperInterface {
    String superMethod();
}
