/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or =at your option) any later version.
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
package org.jboss.as.webservices.dmr;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
interface Constants {
    String ID = "id";
    String CLASS = "class";
    String MODIFY_WSDL_ADDRESS = "modify-wsdl-address";
    String WSDL_HOST = "wsdl-host";
    String WSDL_PORT = "wsdl-port";
    String WSDL_SECURE_PORT = "wsdl-secure-port";
    String WSDL_URI_SCHEME = "wsdl-uri-scheme";
    String ENDPOINT = "endpoint";
    String ENDPOINT_NAME = "name";
    String ENDPOINT_CONTEXT = "context";
    String ENDPOINT_CLASS = "class";
    String ENDPOINT_TYPE = "type";
    String ENDPOINT_WSDL = "wsdl-url";
    String ENDPOINT_CONFIG = "endpoint-config";
    String CLIENT_CONFIG = "client-config";
    String CONFIG_NAME = "config-name";
    String NAME = "name";
    String PROPERTY="property";
    String PROPERTY_NAME="property-name";
    String PROPERTY_VALUE="property-value";
    String PRE_HANDLER_CHAIN="pre-handler-chain";
    String PRE_HANDLER_CHAINS="pre-handler-chains";
    String POST_HANDLER_CHAIN="post-handler-chain";
    String POST_HANDLER_CHAINS="post-handler-chains";
    String HANDLER_CHAIN="handler-chain";
    String PROTOCOL_BINDINGS="protocol-bindings";
    String HANDLER="handler";
    String HANDLER_NAME="handler-name";
    String HANDLER_CLASS="handler-class";
    String VALUE = "value";
    String STATISTICS_ENABLED = "statistics-enabled";
    String WSDL_PATH_REWRITE_RULE = "wsdl-path-rewrite-rule";
}
