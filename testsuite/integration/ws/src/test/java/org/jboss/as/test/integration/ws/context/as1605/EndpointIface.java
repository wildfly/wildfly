/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.context.as1605;

import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * Endpoint interface.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService
@SOAPBinding
public interface EndpointIface {

    String echo(String s);

}
