package org.wildfly.extension.undertow;

import java.util.Collection;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AttributeDefinition;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public interface Handler {
    Collection<AttributeDefinition> getAttributes();

    String getXmlElementName();

    Class<? extends HttpHandler> getHandlerClass();

}
