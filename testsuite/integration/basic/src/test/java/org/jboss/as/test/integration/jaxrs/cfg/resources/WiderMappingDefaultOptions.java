/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("{x:.*}")
public class WiderMappingDefaultOptions {

    @OPTIONS
    public String options() {
        return "hello";
    }
}
