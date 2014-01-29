package com.redhat.gss.extension.requesthandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import com.redhat.gss.redhat_support_lib.api.API;

public class BaseRequestHandler extends SimpleResourceDefinition {

    String operationName = null;
    DefaultOperationDescriptionProvider operationDescriptionProvider = null;
    protected final ParametersValidator validator = new ParametersValidator();

    protected static final SimpleAttributeDefinition username = new SimpleAttributeDefinitionBuilder(
            "username", ModelType.STRING).setAllowExpression(true)
            .setXmlName("username")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    public static final SimpleAttributeDefinition password = new SimpleAttributeDefinitionBuilder(
            "password", ModelType.STRING).setAllowExpression(true)
            .setXmlName("password")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    public static final SimpleAttributeDefinition url = new SimpleAttributeDefinitionBuilder(
            "url", ModelType.STRING, true).setAllowExpression(true)
            .setXmlName("url")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    public static final SimpleAttributeDefinition proxyUser = new SimpleAttributeDefinitionBuilder(
            "proxyUser", ModelType.STRING, true).setAllowExpression(true)
            .setXmlName("proxyUser")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    public static final SimpleAttributeDefinition proxyPassword = new SimpleAttributeDefinitionBuilder(
            "proxyPassword", ModelType.STRING, true).setAllowExpression(true)
            .setXmlName("proxyPassword")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    public static final SimpleAttributeDefinition proxyUrl = new SimpleAttributeDefinitionBuilder(
            "proxyUrl", ModelType.STRING, true).setAllowExpression(true)
            .setXmlName("proxyUrl")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    public static final SimpleAttributeDefinition proxyPort = new SimpleAttributeDefinitionBuilder(
            "proxyPort", ModelType.INT, true).setAllowExpression(true)
            .setXmlName("proxyPort")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public BaseRequestHandler(PathElement pathElement,
            ResourceDescriptionResolver descriptionResolver,
            OperationStepHandler addHandler,
            OperationStepHandler removeHandler, String operationName,
            AttributeDefinition... parameters) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
        this.operationName = operationName;
        createDODP(parameters);
    }

    public void createDODP(AttributeDefinition... parameters) {
        ArrayList<AttributeDefinition> params = new ArrayList<AttributeDefinition>();
        params.add(username);
        params.add(password);
        params.add(url);
        params.add(proxyUser);
        params.add(proxyPassword);
        params.add(proxyUrl);
        params.add(proxyPort);
        params.addAll(Arrays.asList(parameters));
        operationDescriptionProvider = new DefaultOperationDescriptionProvider(
                this.operationName, getResourceDescriptionResolver(),
                params.toArray(new AttributeDefinition[params.size()]));
    }

    public DefaultOperationDescriptionProvider getDODP() {
        return operationDescriptionProvider;
    }

    public API getAPI(OperationContext context, ModelNode operation)
            throws OperationFailedException, MalformedURLException {
        String usernameStr = username.resolveModelAttribute(context, operation)
                .asString();
        String passwordStr = password.resolveModelAttribute(context, operation)
                .asString();
        ModelNode urlStr = url.resolveModelAttribute(context, operation);
        ModelNode proxyUserStr = proxyUser.resolveModelAttribute(context,
                operation);
        ModelNode proxyPasswordStr = proxyPassword.resolveModelAttribute(
                context, operation);
        ModelNode proxyUrlStr = proxyUrl.resolveModelAttribute(context,
                operation);
        ModelNode proxyport = proxyPort
                .resolveModelAttribute(context, operation);
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
