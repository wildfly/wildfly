/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.authorization;

import jakarta.ejb.Local;

/**
 * Simple local interface
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@Local
public interface Simple {
   String testAuthorizedRole();
   String testUnauthorizedRole();
   String testPermitAll();
   String testDenyAll();
}
