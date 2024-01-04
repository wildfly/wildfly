/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas.propagation;

import jakarta.ejb.Remote;

/**
 * @author tmiyar
 *
 */
@Remote
public interface CallerInRole {

    Boolean isCallerInRole(String role);

}
