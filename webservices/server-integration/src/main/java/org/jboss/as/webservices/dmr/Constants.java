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
public final class Constants {
    private Constants() {
        // forbidden inheritance
    }
    public static final String ID = "id";
    public static final String MODIFY_WSDL_ADDRESS = "modify-wsdl-address";
    public static final String WSDL_HOST = "wsdl-host";
    public static final String WSDL_PORT = "wsdl-port";
    public static final String WSDL_SECURE_PORT = "wsdl-secure-port";
    public static final String ENDPOINT = "endpoint";
    public static final String ENDPOINT_NAME = "name";
    public static final String ENDPOINT_CONTEXT = "context";
    public static final String ENDPOINT_CLASS = "class";
    public static final String ENDPOINT_TYPE = "type";
    public static final String ENDPOINT_WSDL = "wsdl-url";
    public static final String ENDPOINT_CONFIG = "endpoint-config";
    public static final String CONFIG_NAME = "config-name";
    public static final String PROPERTY="property";
    public static final String PROPERTY_NAME="property-name";
    public static final String PROPERTY_VALUE="property-value";
    public static final String PRE_HANDLER_CHAIN="pre-handler-chain";
    public static final String PRE_HANDLER_CHAINS="pre-handler-chains";
    public static final String POST_HANDLER_CHAIN="post-handler-chain";
    public static final String POST_HANDLER_CHAINS="post-handler-chains";
    public static final String HANDLER_CHAIN="handler-chain";
    public static final String PROTOCOL_BINDINGS="protocol-bindings";
    public static final String SERVICE_NAME_PATTERN="service-name-pattern";
    public static final String PORT_NAME_PATTERN="port-name-pattern";
    public static final String HANDLER="handler";
    public static final String HANDLER_NAME="handler-name";
    public static final String HANDLER_CLASS="handler-class";
}
