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

package org.jboss.as.jmx;

/**
 * @author Emanuel Muckenhuber
 */
interface CommonAttributes {

    String CONNECTOR = "connector";
    String DEFAULT_EXPRESSION_DOMAIN = "jboss.as.expr";
    String DEFAULT_RESOLVED_DOMAIN = "jboss.as";
    String DOMAIN_NAME = "domain-name";
    String EXPOSE_EXPRESSION_MODEL = "expose-expression-model";
    String EXPOSE_MODEL = "expose-model";
    String EXPOSE_RESOLVED_MODEL = "expose-resolved-model";
    String EXPRESSION = "expression";
    String JMX = "jmx";
    String NAME = "name";
    String SHOW_MODEL = "show-model";
    String PROPER_PROPERTY_FORMAT = "proper-property-format";
    String REGISTRY_BINDING = "registry-binding";
    String REMOTING_CONNECTOR = "remoting-connector";
    String RESOLVED = "resolved";
    String SERVER_BINDING = "server-binding";
    String JMX_CONNECTOR = "jmx-connector";
    String USE_MANAGEMENT_ENDPOINT = "use-management-endpoint";
    String VALUE = "value";
}
