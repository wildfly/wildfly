/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.cfg;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.Path;

@Stateless
@Path("/")
public class EJB_Resource1 {
}
