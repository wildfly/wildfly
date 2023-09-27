/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.enventry.shared;

import jakarta.ejb.Local;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Local
public interface Shared {
    String doit();
}
