/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.xerces.ws;

import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * User: jpai
 */
@WebService
@SOAPBinding
public interface XercesUsageWSEndpoint {

    String parseUsingXerces(String xmlResource);
}
