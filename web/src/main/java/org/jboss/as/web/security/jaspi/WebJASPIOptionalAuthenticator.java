/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.security.jaspi;

import org.apache.catalina.Context;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;
import org.jboss.security.ServerAuthenticationManager;
import org.jboss.security.auth.message.GenericMessageInfo;
import org.jboss.security.plugins.auth.JASPIServerAuthenticationManager;

import org.jboss.as.web.WebLogger;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * <p>
 * This class implements a JASPI authenticator for unprotected resources. In the JASPI Servlet profile, authentication
 * for unprotected resources is optional but it is still allowed. When performed, the JASPI authentication modules must
 * grant access to the unprotected resources irrespective of the caller, which may be anonymous (i.e, no security info
 * supplied).
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class WebJASPIOptionalAuthenticator extends ValveBase {

    private static Logger log = Logger.getLogger("org.jboss.as.web.security");

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        Realm realm = this.getContainer().getRealm();
        // call JASPI only for the unprotected resources - at this point, access to protected resources has already been
        // checked by the web container authenticator.
        if (realm.findSecurityConstraints(request, (Context) this.getContainer()) == null) {

            log.tracef("Invoking optional JASPI authenticator for request context %s", request.getServletPath());

            GenericMessageInfo messageInfo = new GenericMessageInfo();
            messageInfo.setRequestMessage(request);
            messageInfo.setResponseMessage(request.getResponse());

            // authentication here is not mandatory.
            messageInfo.getMap().put("javax.security.auth.message.MessagePolicy.isMandatory", "false");

            WebJASPICallbackHandler cbh = new WebJASPICallbackHandler();
            ServerAuthenticationManager sam = new JASPIServerAuthenticationManager();

            String appContext = request.getLocalName() + " " + request.getContextPath();
            String messageLayer = "HttpServlet";

            boolean isValid = sam.isValid(messageInfo, new Subject(), messageLayer, appContext, cbh);
            if (isValid) {
                WebLogger.WEB_SECURITY_LOGGER.debugf("JASPI validation for unprotected request context %s succeeded", request.getServletPath());
                sam.secureResponse(messageInfo, new Subject(),  messageLayer, appContext, cbh);
            }
            else {
                // just log an error - this situation indicates a problem with the JASPI implementation but the call is
                // safe to proceed to the unprotected resource.
                WebLogger.WEB_SECURITY_LOGGER.failJASPIValidation(request.getServletPath());
            }
        }
        super.getNext().invoke(request, response);
    }
}
