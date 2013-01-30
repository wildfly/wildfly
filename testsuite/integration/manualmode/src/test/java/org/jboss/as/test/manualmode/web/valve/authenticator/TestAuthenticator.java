/*
 * Copyright(c) 2012 Red Hat Middleware, LLC,
 * and individual contributors as indicated by the @authors tag.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library in the file COPYING.LIB;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * @author Jean-Frederic Clere
 */
package org.jboss.as.test.manualmode.web.valve.authenticator;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.Realm;
import org.jboss.logging.Logger;

/*
 * Test Authenticator.
 */
public class TestAuthenticator extends AuthenticatorBase {

    private static Logger log = Logger.getLogger(TestAuthenticator.class);
    
    private String testparam = AuthValveConstants.AUTH_VALVE_DEFAULT_PARAM_VALUE;

    public void setTestparam(String testparam) {
        this.testparam = testparam;
    }

    public String getTestparam() {
        return this.testparam;
    }

    @Override
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {

        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            log.info("User " + principal.getName() + " is already autenticated");
            return true;
        }

        Realm realm = request.getContext().getRealm();
        log.info("Authentication against "+ realm.toString() + " realm: " + realm.getInfo());
        principal = realm.authenticate("user1", "password1");
        if (principal != null) {
            request.setUserPrincipal(principal);
            log.info("Authentication via custom valve authenticator");
            response.addHeader("valve", testparam);
            log.info("Valve " + TestAuthenticator.class.getName() + " was hit and adding header parameter 'authenticated' with value " + testparam);
            return true;
        }
        log.warn("Login via global valve authenticator wasn't successfull");
        return false;
    }
}