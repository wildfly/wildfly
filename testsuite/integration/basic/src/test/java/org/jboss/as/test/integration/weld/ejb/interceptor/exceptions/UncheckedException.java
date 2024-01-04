/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.exceptions;

import jakarta.ejb.ApplicationException;

/**
 * @author Stuart Douglas
 */
@ApplicationException
public class UncheckedException extends RuntimeException {
}
