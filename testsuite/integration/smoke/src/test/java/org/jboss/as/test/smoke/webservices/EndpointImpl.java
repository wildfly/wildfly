/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.webservices;

import jakarta.jws.WebService;

import org.jboss.logging.Logger;

/**
 * A simple endpoint
 *
 * @author alessio.soldano@jboss.com
 * @since 25-Jan-2011
 */
@WebService(serviceName="EndpointService", portName="EndpointPort", endpointInterface = "org.jboss.as.test.smoke.webservices.Endpoint")
public class EndpointImpl {
   // Provide logging
   private static Logger log = Logger.getLogger(EndpointImpl.class);

   public String echo(String input) {
      log.trace("echo: " + input);
      return input;
   }
}
