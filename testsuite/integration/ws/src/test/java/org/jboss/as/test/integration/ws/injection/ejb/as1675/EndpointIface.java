/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.injection.ejb.as1675;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * An endpoint interface.
 *
 * @author <a href="mailto:richard.opalka@jboss.org">Richard Opalka</a>
 */
@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface EndpointIface {
    @WebMethod
    String echo(String s);
}
