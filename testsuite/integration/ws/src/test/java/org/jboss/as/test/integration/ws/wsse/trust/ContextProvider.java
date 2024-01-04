/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust;

public interface ContextProvider {

   String getEjbCallerPrincipalName();
}
