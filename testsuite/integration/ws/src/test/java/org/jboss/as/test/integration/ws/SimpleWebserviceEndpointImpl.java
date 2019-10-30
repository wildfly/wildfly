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

package org.jboss.as.test.integration.ws;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

/**
 * Webservice endpoint implementation.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService(
        endpointInterface = "org.jboss.as.test.integration.ws.SimpleWebserviceEndpointIface",
        targetNamespace = "org.jboss.as.test.integration.ws",
        serviceName = "SimpleService"
)
public class SimpleWebserviceEndpointImpl {

    @Resource
    WebServiceContext ctx;

    // method driven injection, with default name to be computed
    private String string1;
    @Resource(name = "string2")
    private String string2;
    // XML driven injection
    private String string3;

    @Resource
    private void setString1(final String s) {
        string1 = s;
    }

    private boolean postConstructCalled;

    @PostConstruct
    private void init() {
        postConstructCalled = true;
    }

    public String echo(final String s) {
        if (!postConstructCalled) { throw new RuntimeException("@PostConstruct not called"); }
        if (!"Ahoj 1".equals(string1)) { throw new RuntimeException("@Resource String with default name not injected"); }
        if (!"Ahoj 2".equals(string2)) { throw new RuntimeException("@Resource String with explicit name not injected"); }
        if (!"Ahoj 2".equals(string3)) { throw new RuntimeException("@Resource String with DD driven injection not injected"); }
        if (ctx == null) { throw new RuntimeException("@Resource WebServiceContext not injected"); }
        return s;
    }

}
