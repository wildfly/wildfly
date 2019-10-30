/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ws.injection.ejb.as1675;

import org.jboss.ws.api.annotation.WebContext;

import javax.ejb.Stateless;
import javax.jws.WebService;

/**
 * EJB3 bean published as WebService injecting other EJB3 bean and resources.
 *
 * @author <a href="mailto:richard.opalka@jboss.org">Richard Opalka</a>
 */
@Stateless
@WebService(
        name = "EJB3",
        serviceName = "EJB3Service",
        targetNamespace = "http://jbossws.org/as1675",
        endpointInterface = "org.jboss.as.test.integration.ws.injection.ejb.as1675.EndpointIface"
)
@WebContext(
        urlPattern = "/EJB3Service",
        contextRoot = "/as1675"
)
public class EJB3Bean extends AbstractEndpointImpl {
    public String echo(String msg) {
        return super.echo(msg) + ":EJB3Bean";
    }
}
