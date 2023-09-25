/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.remove;

import jakarta.ejb.ApplicationException;

/**
 * User: jpai
 */
@ApplicationException
public class SimpleAppException extends RuntimeException {
}
