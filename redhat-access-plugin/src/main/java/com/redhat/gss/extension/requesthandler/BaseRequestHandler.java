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
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import com.redhat.gss.redhat_support_lib.api.API;

public class BaseRequestHandler {
    protected final ParametersValidator validator = new ParametersValidator();

    protected static final SimpleAttributeDefinition USERNAME = new SimpleAttributeDefinitionBuilder(
            "username", ModelType.STRING).setAllowExpression(true)
            .setXmlName("username")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    protected static final SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinitionBuilder(
            "password", ModelType.STRING).setAllowExpression(true)
            .setXmlName("password")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    protected static final SimpleAttributeDefinition URL = new SimpleAttributeDefinitionBuilder(
            "url", ModelType.STRING, true).setAllowExpression(true)
            .setXmlName("url")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    protected static final SimpleAttributeDefinition PROXYUSER = new SimpleAttributeDefinitionBuilder(
            "proxyUser", ModelType.STRING, true).setAllowExpression(true)
            .setXmlName("proxyUser")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    protected static final SimpleAttributeDefinition PROXYPASSWORD = new SimpleAttributeDefinitionBuilder(
            "proxyPassword", ModelType.STRING, true).setAllowExpression(true)
            .setXmlName("proxyPassword")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    protected static final SimpleAttributeDefinition PROXYURL = new SimpleAttributeDefinitionBuilder(
            "proxyUrl", ModelType.STRING, true).setAllowExpression(true)
            .setXmlName("proxyUrl")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    protected static final SimpleAttributeDefinition PROXYPORT = new SimpleAttributeDefinitionBuilder(
            "proxyPort", ModelType.INT, true).setAllowExpression(true)
            .setXmlName("proxyPort")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

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
            throws OperationFailedException, MalformedURLException {
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
            proxyUrlUrl = new URL(proxyUrlStr.asString());
        }
        int proxyPortInt = -1;
        if (proxyport.isDefined()) {
            proxyPortInt = proxyport.asInt();
        }

        API api = null;
        if (urlStr.isDefined()
                && !urlStr.asString().equals("http://api.access.redhat.com")) {
            api = new API(
                    usernameStr,
                    passwordStr,
                    (urlStr.isDefined() ? urlStr.asString() : null),
                    (proxyUserStr.isDefined() ? proxyUserStr.asString() : null),
                    (proxyPasswordStr.isDefined() ? proxyPasswordStr.asString()
                            : null), proxyUrlUrl, proxyPortInt, "eap", true);
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
