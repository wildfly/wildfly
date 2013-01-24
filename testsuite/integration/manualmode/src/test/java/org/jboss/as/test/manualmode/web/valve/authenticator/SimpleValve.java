/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.manualmode.web.valve.authenticator;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;

public class SimpleValve extends ValveBase {
    private static final Logger log = Logger.getLogger(SimpleValve.class);

    private String testparam = AuthValveConstants.STANDARD_VALVE_DEFAULT_PARAM_VALUE;

    public void setTestparam(String testparam) {
        this.testparam = testparam;
    }
    
    public String getTestparam() {
        return this.testparam;
    }

    public void invoke(Request request, Response response)
        throws IOException, ServletException {
        response.addHeader("valve", testparam);
        log.info("Valve " + SimpleValve.class.getName() + " was hit and adding header parameter 'valve' with value " + testparam);
        getNext().invoke(request, response);
    }
}

