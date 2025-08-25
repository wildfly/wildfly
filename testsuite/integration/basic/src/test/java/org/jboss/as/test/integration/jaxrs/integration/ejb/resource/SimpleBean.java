/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.integration.ejb.resource;

import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ApplicationScoped
public class SimpleBean {

    public String name() {
        return "SimpleBean";
    }
}
