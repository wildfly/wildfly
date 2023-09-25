/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.integration.ejb;

import jakarta.transaction.SystemException;

/**
 * @author Stuart Douglas
 */
public interface EjbInterface {
    String getMessage() throws SystemException;
}
