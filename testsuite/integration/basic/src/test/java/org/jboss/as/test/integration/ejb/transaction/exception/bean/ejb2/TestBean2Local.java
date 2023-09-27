/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2;

import jakarta.ejb.EJBLocalObject;

public interface TestBean2Local extends EJBLocalObject {

    void throwRuntimeException();
}
