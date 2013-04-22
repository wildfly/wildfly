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

package org.jboss.as.camel.utils;

import java.net.URL;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.spring.handler.CamelNamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.UrlResource;

/**
 * A {@link CamelContext} factory utility.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public final class CamelContextFactory {

    // Hide ctor
    private CamelContextFactory() {
    }

    /**
     * Create a {@link SpringCamelContext} from the given input stream
     */
    public static CamelContext createSpringCamelContext(URL contextUrl) throws Exception {
        GenericApplicationContext appContext = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(appContext) {
            @Override
            protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
                NamespaceHandlerResolver defaultResolver = super.createDefaultNamespaceHandlerResolver();
                return new CamelNamespaceHandlerResolver(defaultResolver);
            }
        };
        xmlReader.loadBeanDefinitions(new UrlResource(contextUrl));
        appContext.refresh();
        return SpringCamelContext.springCamelContext(appContext);
    }

    private static class CamelNamespaceHandlerResolver implements NamespaceHandlerResolver {

        private final NamespaceHandlerResolver delegate;
        private final NamespaceHandler camelHandler;

        CamelNamespaceHandlerResolver(NamespaceHandlerResolver delegate) {
            this.delegate = delegate;
            this.camelHandler = new CamelNamespaceHandler();
            this.camelHandler.init();
        }

        @Override
        public NamespaceHandler resolve(String namespaceUri) {
            if ("http://camel.apache.org/schema/spring".equals(namespaceUri)) {
                return camelHandler;
            } else {
                return delegate.resolve(namespaceUri);
            }
        }
    }
}
