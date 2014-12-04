/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package com.redhat.gss.extension.requesthandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import com.redhat.gss.extension.Constants;
import com.redhat.gss.redhat_support_lib.api.API;

public class BaseRequestHandler {
    protected static final SimpleAttributeDefinition USERNAME = new SimpleAttributeDefinitionBuilder(
            "username", ModelType.STRING).build();
    protected static final SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinitionBuilder(
            "password", ModelType.STRING).build();
    protected static final SimpleAttributeDefinition URL = new SimpleAttributeDefinitionBuilder(
            "url", ModelType.STRING, true).build();
    protected static final SimpleAttributeDefinition PROXYUSER = new SimpleAttributeDefinitionBuilder(
            "proxy-user", ModelType.STRING, true).build();
    protected static final SimpleAttributeDefinition PROXYPASSWORD = new SimpleAttributeDefinitionBuilder(
            "proxy-password", ModelType.STRING, true).build();
    protected static final SimpleAttributeDefinition PROXYURL = new SimpleAttributeDefinitionBuilder(
            "proxy-url", ModelType.STRING, true).build();
    protected static final SimpleAttributeDefinition PROXYPORT = new SimpleAttributeDefinitionBuilder(
            "proxy-port", ModelType.INT, true).build();
    // Not included by default
    protected static final SimpleAttributeDefinition CASENUMBER = new SimpleAttributeDefinitionBuilder(
            "case-number", ModelType.STRING).build();

    protected static AttributeDefinition[] getParameters(
            AttributeDefinition... parameters) {
        ArrayList<AttributeDefinition> params = new ArrayList<AttributeDefinition>();
        params.add(USERNAME);
        params.add(PASSWORD);
        params.add(URL);
        params.add(PROXYUSER);
        params.add(PROXYPASSWORD);
        params.add(PROXYURL);
        params.add(PROXYPORT);
        params.addAll(Arrays.asList(parameters));
        return params.toArray(new AttributeDefinition[params.size()]);
    }

    public API getAPI(OperationContext context, ModelNode operation)
            throws OperationFailedException {
        String usernameStr = USERNAME.resolveModelAttribute(context, operation)
                .asString();
        String passwordStr = PASSWORD.resolveModelAttribute(context, operation)
                .asString();
        ModelNode urlStr = URL.resolveModelAttribute(context, operation);
        ModelNode proxyUserStr = PROXYUSER.resolveModelAttribute(context,
                operation);
        ModelNode proxyPasswordStr = PROXYPASSWORD.resolveModelAttribute(
                context, operation);
        ModelNode proxyUrlStr = PROXYURL.resolveModelAttribute(context,
                operation);
        ModelNode proxyport = PROXYPORT.resolveModelAttribute(context,
                operation);
        URL proxyUrlUrl = null;
        if (proxyUrlStr.isDefined()) {
            try {
                proxyUrlUrl = new URL(proxyUrlStr.asString());
            } catch (MalformedURLException e) {
                throw new OperationFailedException(e.getLocalizedMessage());
            }
        }
        int proxyPortInt = -1;
        if (proxyport.isDefined()) {
            proxyPortInt = proxyport.asInt();
        }

        API api = null;
        if (urlStr.isDefined()
                && !urlStr.asString().equals(Constants.RHAccessURL)) {
            api = new API(
                    usernameStr,
                    passwordStr,
                    (urlStr.isDefined() ? urlStr.asString() : null),
                    (proxyUserStr.isDefined() ? proxyUserStr.asString() : null),
                    (proxyPasswordStr.isDefined() ? proxyPasswordStr.asString()
                            : null), proxyUrlUrl, proxyPortInt, Constants.userAgent, true);
        } else {
            api = new API(
                    usernameStr,
                    passwordStr,
                    (urlStr.isDefined() ? urlStr.asString() : null),
                    (proxyUserStr.isDefined() ? proxyUserStr.asString() : null),
                    (proxyPasswordStr.isDefined() ? proxyPasswordStr.asString()
                            : null), proxyUrlUrl, proxyPortInt, "eap");
        }
        return api;
    }
}
