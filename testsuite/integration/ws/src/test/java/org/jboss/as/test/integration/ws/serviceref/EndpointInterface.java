/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.serviceref;

import jakarta.ejb.Remote;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService(targetNamespace = "http://www.openuri.org/2004/04/HelloWorld")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@Remote
public interface EndpointInterface {
    String echo(String input);
}
