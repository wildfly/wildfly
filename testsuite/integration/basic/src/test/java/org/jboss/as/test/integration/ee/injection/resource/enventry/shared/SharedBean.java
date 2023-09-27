/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.enventry.shared;

import jakarta.ejb.Stateless;
import jakarta.annotation.Resource;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Stateless
public class SharedBean implements Shared {
    @Resource(name = "strWho")
    String strWho;

    public String doit() {
        return strWho;
    }
}
