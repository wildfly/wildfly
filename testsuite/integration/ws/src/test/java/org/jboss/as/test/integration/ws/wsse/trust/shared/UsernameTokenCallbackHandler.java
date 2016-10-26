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
package org.jboss.as.test.integration.ws.wsse.trust.shared;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.delegation.DelegationCallback;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.Map;

/**
 * A utility to provide the 3 different input parameter types for jaxws property
 * "ws-security.sts.token.act-as" and "ws-security.sts.token.on-behalf-of".
 * This implementation obtains a username and password via the jaxws property
 * "ws-security.username" and "ws-security.password" respectively, as defined
 * in SecurityConstants.  It creates a wss UsernameToken to be used as the
 * delegation token.
 * <p/>
 * User: rsearls
 * Date: 2/3/14
 */

public class UsernameTokenCallbackHandler implements CallbackHandler {

    public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof DelegationCallback) {
                DelegationCallback callback = (DelegationCallback) callbacks[i];
                Message message = callback.getCurrentMessage();

                String username =
                        (String) message.getContextualProperty(SecurityConstants.USERNAME);
                String password =
                        (String) message.getContextualProperty(SecurityConstants.PASSWORD);
                if (username != null) {
                    Node contentNode = message.getContent(Node.class);
                    Document doc = null;
                    if (contentNode != null) {
                        doc = contentNode.getOwnerDocument();
                    } else {
                        doc = DOMUtils.createDocument();
                    }
                    UsernameToken usernameToken = createWSSEUsernameToken(username, password, doc);
                    callback.setToken(usernameToken.getElement());
                }
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }

    /**
     * Provide UsernameToken as a string.
     *
     * @param ctx
     * @return
     */
    public String getUsernameTokenString(Map<String, Object> ctx) {
        Document doc = DOMUtils.createDocument();
        String result = null;
        String username = (String) ctx.get(SecurityConstants.USERNAME);
        String password = (String) ctx.get(SecurityConstants.PASSWORD);
        if (username != null) {
            UsernameToken usernameToken = createWSSEUsernameToken(username, password, doc);
            result = toString(usernameToken.getElement().getFirstChild().getParentNode());
        }
        return result;
    }

    /**
     * @param username
     * @param password
     * @return
     */
    public String getUsernameTokenString(String username, String password) {
        Document doc = DOMUtils.createDocument();
        String result = null;
        if (username != null) {
            UsernameToken usernameToken = createWSSEUsernameToken(username, password, doc);
            result = toString(usernameToken.getElement().getFirstChild().getParentNode());
        }
        return result;
    }

    /**
     * Provide UsernameToken as a DOM Element.
     *
     * @param ctx
     * @return
     */
    public Element getUsernameTokenElement(Map<String, Object> ctx) {
        Document doc = DOMUtils.createDocument();
        Element result = null;
        UsernameToken usernameToken = null;
        String username = (String) ctx.get(SecurityConstants.USERNAME);
        String password = (String) ctx.get(SecurityConstants.PASSWORD);
        if (username != null) {
            usernameToken = createWSSEUsernameToken(username, password, doc);
            result = usernameToken.getElement();
        }
        return result;
    }

    /**
     * @param username
     * @param password
     * @return
     */
    public Element getUsernameTokenElement(String username, String password) {
        Document doc = DOMUtils.createDocument();
        Element result = null;
        UsernameToken usernameToken = null;
        if (username != null) {
            usernameToken = createWSSEUsernameToken(username, password, doc);
            result = usernameToken.getElement();
        }
        return result;
    }

    private UsernameToken createWSSEUsernameToken(String username, String password, Document doc) {

        UsernameToken usernameToken = new UsernameToken(true, doc,
                (password == null) ? null : WSConstants.PASSWORD_TEXT);
        usernameToken.setName(username);
        usernameToken.addWSUNamespace();
        usernameToken.addWSSENamespace();
        usernameToken.setID("id-" + username);

        if (password != null) {
            usernameToken.setPassword(password);
        }

        return usernameToken;
    }


    private String toString(Node node) {
        String str = null;

        if (node != null) {
            DOMImplementationLS lsImpl = (DOMImplementationLS)
                    node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
            LSSerializer serializer = lsImpl.createLSSerializer();
            serializer.getDomConfig().setParameter("xml-declaration", false); //by default its true, so set it to false to get String without xml-declaration
            str = serializer.writeToString(node);
        }
        return str;
    }

}