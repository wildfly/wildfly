/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateless.callerprincipal;

public interface TestEJB3Remote {

   boolean isCallerInRole(String role) throws Exception;

}