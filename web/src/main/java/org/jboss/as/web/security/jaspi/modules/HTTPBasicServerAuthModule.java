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

package org.jboss.as.web.security.jaspi.modules;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.Base64;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.jboss.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

/**
 * <p>
 * This class implements a JASPI {@code ServerAuthModule} that handles the HTTP BASIC authentication.
 * </p>
 *
 * @author <a href="mailto:Anil.Saldhana@redhat.com">Anil Saldhana</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class HTTPBasicServerAuthModule extends WebServerAuthModule {

    private static Logger log = Logger.getLogger("org.jboss.as.web.security");

    protected Context context;

    protected boolean cache = false;

    /**
     * Authenticate bytes.
     */
    public static final byte[] AUTHENTICATE_BYTES = {
            (byte) 'W',
            (byte) 'W',
            (byte) 'W',
            (byte) '-',
            (byte) 'A',
            (byte) 'u',
            (byte) 't',
            (byte) 'h',
            (byte) 'e',
            (byte) 'n',
            (byte) 't',
            (byte) 'i',
            (byte) 'c',
            (byte) 'a',
            (byte) 't',
            (byte) 'e'
    };


    protected String delegatingLoginContextName = null;

    /**
     * <p>
     * Creates an instance of {@code HTTPBasicServerAuthModule}.
     * </p>
     */
    public HTTPBasicServerAuthModule() {
    }

    /**
     * <p>
     * Creates an instance of {@code HTTPBasicServerAuthModule} with the specified delegating login context name.
     * </p>
     *
     * @param delegatingLoginContextName the name of the login context configuration that contains the JAAS modules that
     *                                   are to be called by this module.
     */
    public HTTPBasicServerAuthModule(String delegatingLoginContextName) {
        super();
        this.delegatingLoginContextName = delegatingLoginContextName;
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject)
            throws AuthException {
        // do nothing, just return SUCCESS.
        return AuthStatus.SUCCESS;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        Request request = (Request) messageInfo.getRequestMessage();
        Response response = (Response) messageInfo.getResponseMessage();

        Principal principal;
        context = request.getContext();
        LoginConfig config = context.getLoginConfig();

        // validate any credentials already included with this request.
        String username = null;
        String password = null;

        MessageBytes authorization = request.getCoyoteRequest().getMimeHeaders().getValue("authorization");

        if (authorization != null) {
            authorization.toBytes();
            ByteChunk authorizationBC = authorization.getByteChunk();

            if (authorizationBC.startsWithIgnoreCase("basic ", 0)) {
                authorizationBC.setOffset(authorizationBC.getOffset() + 6);
                CharChunk authorizationCC = authorization.getCharChunk();
                Base64.decode(authorizationBC, authorizationCC);

                // get username and password from the authorization char chunk.
                int colon = authorizationCC.indexOf(':');
                if (colon < 0) {
                    username = authorizationCC.toString();
                } else {
                    char[] buf = authorizationCC.getBuffer();
                    username = new String(buf, 0, colon);
                    password = new String(buf, colon + 1, authorizationCC.getEnd() - colon - 1);
                }

                authorizationBC.setOffset(authorizationBC.getOffset() - 6);
            }

            principal = context.getRealm().authenticate(username, password);
            if (principal != null) {
                registerWithCallbackHandler(principal, username, password);

                // register(request, response, principal, Constants.BASIC_METHOD, username, password);
                return AuthStatus.SUCCESS;
            }
        }

        // send an "unauthorized" response and an appropriate challenge.
        MessageBytes authenticate = response.getCoyoteResponse().getMimeHeaders().
                addValue(AUTHENTICATE_BYTES, 0, AUTHENTICATE_BYTES.length);

        CharChunk authenticateCC = authenticate.getCharChunk();
        try {
            authenticateCC.append("Basic realm=\"");
            if (config.getRealmName() == null) {
                authenticateCC.append(request.getServerName());
                authenticateCC.append(':');
                authenticateCC.append(Integer.toString(request.getServerPort()));
            } else {
                authenticateCC.append(config.getRealmName());
            }
            authenticateCC.append('\"');
            authenticate.toChars();

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IOException e) {
            // Ignore IOException here (client disconnect)
        }

        return AuthStatus.FAILURE;
    }
}