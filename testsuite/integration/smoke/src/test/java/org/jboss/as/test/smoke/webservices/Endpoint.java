/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.webservices;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService (name="Endpoint")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface Endpoint {
   @WebMethod(operationName = "echoString", action = "urn:EchoString")
   String echo(String input);
}
