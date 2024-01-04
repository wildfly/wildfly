/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.security.Principal;

public interface ISLSBWithoutSecurityDomain  {
    Principal getCallerPrincipal();
}
