/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws;

import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * Webservice endpoint interface.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService
@SOAPBinding
public interface SimpleWebserviceEndpointIface {

    String echo(String s);

}
