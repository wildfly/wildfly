package org.wildfly.extension.undertow.filters;

import java.util.Collection;
import java.util.Collections;

import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class BasicAuthHandler extends Filter {

    private static final AttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder("security-domain", ModelType.STRING)
            .setAllowNull(false)
            .build();

    public BasicAuthHandler() {
        super("basic-auth");
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.singleton(SECURITY_DOMAIN);
    }

    @Override
    public Class<? extends HttpHandler> getHandlerClass() {
        return AuthenticationCallHandler.class;
    }

}
