/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.schemalocations;

import jakarta.jws.WebService;

/**
 * Simple WS endpoint
 *
 * @author Tomas Hofman (thofman@redhat.com)
 */
@WebService(
        targetNamespace = "http://jbossws.org/SchemaLocationsRewrite",
        serviceName = "SimpleService",
        wsdlLocation = "WEB-INF/wsdl/SimpleService.wsdl"
)
public class SimpleService {

    public String echo(final String s) {
        return s;
    }

}
