/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.enventry.shared;

import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Stateless
public class BEJBBean implements BEJB {
    @EJB
    Shared shared;

    public String doit() {
        return shared.doit();
    }
}
