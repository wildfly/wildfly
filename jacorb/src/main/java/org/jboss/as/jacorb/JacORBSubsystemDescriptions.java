/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.jacorb.JacORBAttribute.*;
import static org.jboss.as.jacorb.JacORBElement.*;


/**
 * <p>
 * Collection of descriptions for the JacORB subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JacORBSubsystemDescriptions {

    static final String RESOURCE_NAME = JacORBSubsystemDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystem(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    private static class Descriptions {

        static ModelNode getSubsystem(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode subsystem = new ModelNode();

            subsystem.get(DESCRIPTION).set(bundle.getString("jacorb"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(JacORBNamespace.CURRENT.getUriString());

            // describe the orb sub-element.
            subsystem.get(CHILDREN, ORB_CONFIG.getLocalName()).set(getORBConfiguration(locale));
            // describe the poa sub-element.
            subsystem.get(CHILDREN, POA_CONFIG.getLocalName()).set(getPOAConfiguration(locale));
            // describe the interop sub-element.
            subsystem.get(CHILDREN, INTEROP_CONFIG.getLocalName()).set(getInteropConfiguration(locale));
            // describe the security sub-element.
            subsystem.get(CHILDREN, SECURITY_CONFIG.getLocalName()).set(getSecurityConfiguration(locale));
            // describe the property sub-elements.
            subsystem.get(CHILDREN, PROPERTY_CONFIG.getLocalName()).set(getPropertyConfiguration(locale));
            // describe the initializers sub-element.
            subsystem.get(CHILDREN, INITIALIZERS_CONFIG.getLocalName()).set(getInitializersConfiguration(locale));

            return subsystem;
        }

        static ModelNode getORBConfiguration(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("orb.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // orb name attribute.
            node.get(ATTRIBUTES, ORB_NAME.getLocalName(), DESCRIPTION).set(bundle.getString("orb.config.name"));
            node.get(ATTRIBUTES, ORB_NAME.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ORB_NAME.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_NAME.getLocalName(), DEFAULT).set("JBoss");
            // orb print-version attribute.
            node.get(ATTRIBUTES, ORB_PRINT_VERSION.getLocalName(), DESCRIPTION).set(bundle.getString("orb.config.print-version"));
            node.get(ATTRIBUTES, ORB_PRINT_VERSION.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ORB_PRINT_VERSION.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_PRINT_VERSION.getLocalName(), DEFAULT).set("off");
            // orb use-imr attribute.
            node.get(ATTRIBUTES, ORB_USE_IMR.getLocalName(), DESCRIPTION).set(bundle.getString("orb.config.use-imr"));
            node.get(ATTRIBUTES, ORB_USE_IMR.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ORB_USE_IMR.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_USE_IMR.getLocalName(), DEFAULT).set("off");
            // orb use-bom attribute.
            node.get(ATTRIBUTES, ORB_USE_BOM.getLocalName(), DESCRIPTION).set(bundle.getString("orb.config.use-bom"));
            node.get(ATTRIBUTES, ORB_USE_BOM.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ORB_USE_BOM.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_USE_BOM.getLocalName(), DEFAULT).set("off");
            // orb cache-typecodes attribute.
            node.get(ATTRIBUTES, ORB_CACHE_TYPECODES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.config.cache-typecodes"));
            node.get(ATTRIBUTES, ORB_CACHE_TYPECODES.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ORB_CACHE_TYPECODES.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CACHE_TYPECODES.getLocalName(), DEFAULT).set("off");
            // orb cache-poa-names attribute.
            node.get(ATTRIBUTES, ORB_CACHE_POA_NAMES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.config.cache-poa-names"));
            node.get(ATTRIBUTES, ORB_CACHE_POA_NAMES.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ORB_CACHE_POA_NAMES.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CACHE_POA_NAMES.getLocalName(), DEFAULT).set("off");
            // orb giop-minor-version attribute.
            node.get(ATTRIBUTES, ORB_GIOP_MINOR_VERSION.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.config.giop-minor-version"));
            node.get(ATTRIBUTES, ORB_GIOP_MINOR_VERSION.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ORB_GIOP_MINOR_VERSION.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_GIOP_MINOR_VERSION.getLocalName(), DEFAULT).set(2);

            // describe the orb connection and naming sub-elements.
            node.get(CHILDREN, ORB_CONNECTION_CONFIG.getLocalName()).set(getORBConnectionConfiguration(locale));
            node.get(CHILDREN, ORB_NAMING_CONFIG.getLocalName()).set(getORBNamingConfiguration(locale));

            return node;
        }

        static ModelNode getORBConnectionConfiguration(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("orb.conn.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // connection retries attribute.
            node.get(ATTRIBUTES, ORB_CONN_RETRIES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.retries"));
            node.get(ATTRIBUTES, ORB_CONN_RETRIES.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ORB_CONN_RETRIES.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CONN_RETRIES.getLocalName(), DEFAULT).set(5);
            // connection retry-interval attribute.
            node.get(ATTRIBUTES, ORB_CONN_RETRY_INTERVAL.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.retry-interval"));
            node.get(ATTRIBUTES, ORB_CONN_RETRY_INTERVAL.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ORB_CONN_RETRY_INTERVAL.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CONN_RETRY_INTERVAL.getLocalName(), DEFAULT).set(500);
            // connection client-timeout attribute.
            node.get(ATTRIBUTES, ORB_CONN_CLIENT_TIMEOUT.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.client-timeout"));
            node.get(ATTRIBUTES, ORB_CONN_CLIENT_TIMEOUT.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ORB_CONN_CLIENT_TIMEOUT.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CONN_CLIENT_TIMEOUT.getLocalName(), DEFAULT).set(0);
            // connection server-timeout attribute.
            node.get(ATTRIBUTES, ORB_CONN_SERVER_TIMEOUT.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.server-timeout"));
            node.get(ATTRIBUTES, ORB_CONN_SERVER_TIMEOUT.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ORB_CONN_SERVER_TIMEOUT.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CONN_SERVER_TIMEOUT.getLocalName(), DEFAULT).set(0);
            // connection max-server-connections attribute.
            node.get(ATTRIBUTES, ORB_CONN_MAX_SERVER_CONNECTIONS.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.max-server-connections"));
            node.get(ATTRIBUTES, ORB_CONN_MAX_SERVER_CONNECTIONS.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ORB_CONN_MAX_SERVER_CONNECTIONS.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CONN_MAX_SERVER_CONNECTIONS.getLocalName(), DEFAULT).set(Integer.MAX_VALUE);
            // connection max-managed-buf-size attribute.
            node.get(ATTRIBUTES, ORB_CONN_MAX_MANAGED_BUF_SIZE.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.max-managed-buf-size"));
            node.get(ATTRIBUTES, ORB_CONN_MAX_MANAGED_BUF_SIZE.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ORB_CONN_MAX_MANAGED_BUF_SIZE.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CONN_MAX_MANAGED_BUF_SIZE.getLocalName(), DEFAULT).set(24);
            // connection outbuf-size attribute.
            node.get(ATTRIBUTES, ORB_CONN_OUTBUF_SIZE.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.outbuf-size"));
            node.get(ATTRIBUTES, ORB_CONN_OUTBUF_SIZE.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ORB_CONN_OUTBUF_SIZE.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CONN_OUTBUF_SIZE.getLocalName(), DEFAULT).set(2048);
            // connection outbuf-cache_timeout attribute.
            node.get(ATTRIBUTES, ORB_CONN_OUTBUF_CACHE_TIMEOUT.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.outbuf-cache-timeout"));
            node.get(ATTRIBUTES, ORB_CONN_OUTBUF_CACHE_TIMEOUT.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ORB_CONN_OUTBUF_CACHE_TIMEOUT.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_CONN_OUTBUF_CACHE_TIMEOUT.getLocalName(), DEFAULT).set(-1);

            return node;
        }

        static ModelNode getORBNamingConfiguration(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("orb.naming.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // naming root-context attribute.
            node.get(ATTRIBUTES, ORB_NAMING_ROOT_CONTEXT.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.naming.config.root-context"));
            node.get(ATTRIBUTES, ORB_NAMING_ROOT_CONTEXT.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ORB_NAMING_ROOT_CONTEXT.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_NAMING_ROOT_CONTEXT.getLocalName(), DEFAULT).set("JBoss/Naming/root");
            // naming export-corbaloc attribute.
            node.get(ATTRIBUTES, ORB_NAMING_EXPORT_CORBALOC.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.naming.config.export-corbaloc"));
            node.get(ATTRIBUTES, ORB_NAMING_EXPORT_CORBALOC.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ORB_NAMING_EXPORT_CORBALOC.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, ORB_NAMING_EXPORT_CORBALOC.getLocalName(), DEFAULT).set("on");

            return node;
        }

        static ModelNode getPOAConfiguration(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("poa.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // poa monitoring attribute.
            node.get(ATTRIBUTES, POA_MONITORING.getLocalName(), DESCRIPTION).set(bundle.getString("poa.config.monitoring"));
            node.get(ATTRIBUTES, POA_MONITORING.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, POA_MONITORING.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, POA_MONITORING.getLocalName(), DEFAULT).set("off");
            // poa queue-wait attribute.
            node.get(ATTRIBUTES, POA_QUEUE_WAIT.getLocalName(), DESCRIPTION).set(bundle.getString("poa.config.queue-wait"));
            node.get(ATTRIBUTES, POA_QUEUE_WAIT.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, POA_QUEUE_WAIT.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, POA_QUEUE_WAIT.getLocalName(), DEFAULT).set("off");
            // poa queue-min attribute.
            node.get(ATTRIBUTES, POA_QUEUE_MIN.getLocalName(), DESCRIPTION).set(bundle.getString("poa.config.queue-min"));
            node.get(ATTRIBUTES, POA_QUEUE_MIN.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, POA_QUEUE_MIN.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, POA_QUEUE_MIN.getLocalName(), DEFAULT).set(10);
            // poa queue-max attribute.
            node.get(ATTRIBUTES, POA_QUEUE_MAX.getLocalName(), DESCRIPTION).set(bundle.getString("poa.config.queue-max"));
            node.get(ATTRIBUTES, POA_QUEUE_MAX.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, POA_QUEUE_MAX.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, POA_QUEUE_MAX.getLocalName(), DEFAULT).set(100);

            // describe the poa request-processors sub-element.
            node.get(POA_REQUEST_PROC_CONFIG.getLocalName()).set(getPOARequestProcessorsConfiguration(locale));

            return node;
        }

        static ModelNode getPOARequestProcessorsConfiguration(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("poa.request-processors.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // request-processors pool-size attribute.
            node.get(ATTRIBUTES, POA_REQUEST_PROC_POOL_SIZE.getLocalName(), DESCRIPTION).
                    set(bundle.getString("poa.request-processors.config.pool-size"));
            node.get(ATTRIBUTES, POA_REQUEST_PROC_POOL_SIZE.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, POA_REQUEST_PROC_POOL_SIZE.getLocalName(), REQUIRED).set(true);
            node.get(ATTRIBUTES, POA_REQUEST_PROC_POOL_SIZE.getLocalName(), DEFAULT).set(5);
            // request-processors max_threads attribute.
            node.get(ATTRIBUTES, POA_REQUEST_PROC_MAX_THREADS.getLocalName(), DESCRIPTION).
                    set(bundle.getString("poa.request-processors.config.max-threads"));
            node.get(ATTRIBUTES, POA_REQUEST_PROC_MAX_THREADS.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, POA_REQUEST_PROC_MAX_THREADS.getLocalName(), REQUIRED).set(true);
            node.get(ATTRIBUTES, POA_REQUEST_PROC_MAX_THREADS.getLocalName(), DEFAULT).set(20);

            return node;
        }

        static ModelNode getInteropConfiguration(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("interop.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // interop sun attribute.
            node.get(ATTRIBUTES, INTEROP_SUN.getLocalName(), DESCRIPTION).set(bundle.getString("interop.config.sun"));
            node.get(ATTRIBUTES, INTEROP_SUN.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, INTEROP_SUN.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, INTEROP_SUN.getLocalName(), DEFAULT).set("on");
            // interop comet attribute.
            node.get(ATTRIBUTES, INTEROP_COMET.getLocalName(), DESCRIPTION).set(bundle.getString("interop.config.comet"));
            node.get(ATTRIBUTES, INTEROP_COMET.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, INTEROP_COMET.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, INTEROP_COMET.getLocalName(), DEFAULT).set("off");
            // interop chunk-custom-rmi-valuetypes attribute.
            node.get(ATTRIBUTES, INTEROP_CHUNK_RMI_VALUETYPES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("interop.config.chunk-custom-rmi-valuetypes"));
            node.get(ATTRIBUTES, INTEROP_CHUNK_RMI_VALUETYPES.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, INTEROP_CHUNK_RMI_VALUETYPES.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, INTEROP_CHUNK_RMI_VALUETYPES.getLocalName(), DEFAULT).set("on");
            // interop lax-boolean-encoding attribute.
            node.get(ATTRIBUTES, INTEROP_LAX_BOOLEAN_ENCODING.getLocalName(), DESCRIPTION).
                    set(bundle.getString("interop.config.lax-boolean-encoding"));
            node.get(ATTRIBUTES, INTEROP_LAX_BOOLEAN_ENCODING.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, INTEROP_LAX_BOOLEAN_ENCODING.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, INTEROP_LAX_BOOLEAN_ENCODING.getLocalName(), DEFAULT).set("off");
            // interop indirection-encoding-disable attribute.
            node.get(ATTRIBUTES, INTEROP_INDIRECTION_ENCODING_DISABLE.getLocalName(), DESCRIPTION).
                    set(bundle.getString("interop.config.indirection-encoding-disable"));
            node.get(ATTRIBUTES, INTEROP_INDIRECTION_ENCODING_DISABLE.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, INTEROP_INDIRECTION_ENCODING_DISABLE.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, INTEROP_INDIRECTION_ENCODING_DISABLE.getLocalName(), DEFAULT).set("off");
            // interop strict-check-on-tc-creation attribute.
            node.get(ATTRIBUTES, INTEROP_STRICT_CHECK_ON_TC_CREATION.getLocalName(), DESCRIPTION).
                    set(bundle.getString("interop.config.strict-check-on-tc-creation"));
            node.get(ATTRIBUTES, INTEROP_STRICT_CHECK_ON_TC_CREATION.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, INTEROP_STRICT_CHECK_ON_TC_CREATION.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, INTEROP_STRICT_CHECK_ON_TC_CREATION.getLocalName(), DEFAULT).set("off");

            return node;
        }

        static ModelNode getSecurityConfiguration(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("security.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // security support-ssl attribute.
            node.get(ATTRIBUTES, SECURITY_SUPPORT_SSL.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.support-ssl"));
            node.get(ATTRIBUTES, SECURITY_SUPPORT_SSL.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, SECURITY_SUPPORT_SSL.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, SECURITY_SUPPORT_SSL.getLocalName(), DEFAULT).set("off");
            // security add-component-via-interceptor attribute.
            node.get(ATTRIBUTES, SECURITY_ADD_COMPONENT_INTERCEPTOR.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.add-component-via-interceptor"));
            node.get(ATTRIBUTES, SECURITY_ADD_COMPONENT_INTERCEPTOR.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, SECURITY_ADD_COMPONENT_INTERCEPTOR.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, SECURITY_ADD_COMPONENT_INTERCEPTOR.getLocalName(), DEFAULT).set("on");
            // security client-supports attribute.
            node.get(ATTRIBUTES, SECURITY_CLIENT_SUPPORTS.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.client-supports"));
            node.get(ATTRIBUTES, SECURITY_CLIENT_SUPPORTS.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, SECURITY_CLIENT_SUPPORTS.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, SECURITY_CLIENT_SUPPORTS.getLocalName(), DEFAULT).set(60);
            // security client-requires attribute.
            node.get(ATTRIBUTES, SECURITY_CLIENT_REQUIRES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.client-requires"));
            node.get(ATTRIBUTES, SECURITY_CLIENT_REQUIRES.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, SECURITY_CLIENT_REQUIRES.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, SECURITY_CLIENT_REQUIRES.getLocalName(), DEFAULT).set(0);
            // security server-supports attribute.
            node.get(ATTRIBUTES, SECURITY_SERVER_SUPPORTS.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.server-supports"));
            node.get(ATTRIBUTES, SECURITY_SERVER_SUPPORTS.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, SECURITY_SERVER_SUPPORTS.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, SECURITY_SERVER_SUPPORTS.getLocalName(), DEFAULT).set(60);
            // security server-requires attribute.
            node.get(ATTRIBUTES, SECURITY_SERVER_REQUIRES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.server-requires"));
            node.get(ATTRIBUTES, SECURITY_SERVER_REQUIRES.getLocalName(), TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, SECURITY_SERVER_REQUIRES.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, SECURITY_SERVER_REQUIRES.getLocalName(), DEFAULT).set(0);
            // security use-domain-socket-factory attribute.
            node.get(ATTRIBUTES, SECURITY_USE_DOMAIN_SF.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.use-domain-socket-factory"));
            node.get(ATTRIBUTES, SECURITY_USE_DOMAIN_SF.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, SECURITY_USE_DOMAIN_SF.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, SECURITY_USE_DOMAIN_SF.getLocalName(), DEFAULT).set("off");
            // security use-domain-server-socket-factory attribute.
            node.get(ATTRIBUTES, SECURITY_USE_DOMAIN_SSF.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.use-domain-server-socket-factory"));
            node.get(ATTRIBUTES, SECURITY_USE_DOMAIN_SSF.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, SECURITY_USE_DOMAIN_SSF.getLocalName(), REQUIRED).set(false);
            node.get(ATTRIBUTES, SECURITY_USE_DOMAIN_SSF.getLocalName(), DEFAULT).set("off");

            return node;
        }

        static ModelNode getPropertyConfiguration(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("property.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(Integer.MAX_VALUE);

            // property key and value attributes.
            node.get(ATTRIBUTES, PROP_KEY.getLocalName(), DESCRIPTION).set(bundle.getString("property.config.key"));
            node.get(ATTRIBUTES, PROP_KEY.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, PROP_KEY.getLocalName(), REQUIRED).set(true);
            node.get(ATTRIBUTES, PROP_VALUE.getLocalName(), DESCRIPTION).set(bundle.getString("property.config.value"));
            node.get(ATTRIBUTES, PROP_VALUE.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, PROP_VALUE.getLocalName(), REQUIRED).set(true);

            return node;
        }

        static ModelNode getInitializersConfiguration(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.STRING);
            node.get(DESCRIPTION).set(bundle.getString("initializers.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);
            node.get(DEFAULT).set("Codebase,CSIv2,SAS");

            return node;
        }

        static ModelNode getSubsystemAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode op = new ModelNode();

            op.get(OPERATION_NAME).set(ADD);
            op.get(DESCRIPTION).set(bundle.getString("jacorb.add"));

            // describe the orb sub-element.
            op.get(CHILDREN, ORB_CONFIG.getLocalName()).set(getORBConfigurationAdd(locale));
            // describe the poa sub-element.
            op.get(CHILDREN, POA_CONFIG.getLocalName()).set(getPOAConfigurationAdd(locale));
            // describe the interop sub-element.
            op.get(CHILDREN, INTEROP_CONFIG.getLocalName()).set(getInteropConfigurationAdd(locale));
            // describe the security sub-element.
            op.get(CHILDREN, SECURITY_CONFIG.getLocalName()).set(getSecurityConfigurationAdd(locale));
            // describe the property sub-elements.
            op.get(CHILDREN, PROPERTY_CONFIG.getLocalName()).set(getPropertyConfigurationAdd(locale));
            // describe the initializers sub-element.
            op.get(CHILDREN, INITIALIZERS_CONFIG.getLocalName()).set(getInitializersConfiguration(locale));

            op.get(REPLY_PROPERTIES).setEmptyObject();
            return op;
        }

        static ModelNode getORBConfigurationAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("orb.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // orb name attribute.
            node.get(REQUEST_PROPERTIES, ORB_NAME.getLocalName(), DESCRIPTION).set(bundle.getString("orb.config.name"));
            node.get(REQUEST_PROPERTIES, ORB_NAME.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ORB_NAME.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_NAME.getLocalName(), DEFAULT).set("JBoss");
            // orb print-version attribute.
            node.get(REQUEST_PROPERTIES, ORB_PRINT_VERSION.getLocalName(), DESCRIPTION).set(bundle.getString("orb.config.print-version"));
            node.get(REQUEST_PROPERTIES, ORB_PRINT_VERSION.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ORB_PRINT_VERSION.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_PRINT_VERSION.getLocalName(), DEFAULT).set("off");
            // orb use-imr attribute.
            node.get(REQUEST_PROPERTIES, ORB_USE_IMR.getLocalName(), DESCRIPTION).set(bundle.getString("orb.config.use-imr"));
            node.get(REQUEST_PROPERTIES, ORB_USE_IMR.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ORB_USE_IMR.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_USE_IMR.getLocalName(), DEFAULT).set("off");
            // orb use-bom attribute.
            node.get(REQUEST_PROPERTIES, ORB_USE_BOM.getLocalName(), DESCRIPTION).set(bundle.getString("orb.config.use-bom"));
            node.get(REQUEST_PROPERTIES, ORB_USE_BOM.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ORB_USE_BOM.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_USE_BOM.getLocalName(), DEFAULT).set("off");
            // orb cache-typecodes attribute.
            node.get(REQUEST_PROPERTIES, ORB_CACHE_TYPECODES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.config.cache-typecodes"));
            node.get(REQUEST_PROPERTIES, ORB_CACHE_TYPECODES.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ORB_CACHE_TYPECODES.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CACHE_TYPECODES.getLocalName(), DEFAULT).set("off");
            // orb cache-poa-names attribute.
            node.get(REQUEST_PROPERTIES, ORB_CACHE_POA_NAMES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.config.cache-poa-names"));
            node.get(REQUEST_PROPERTIES, ORB_CACHE_POA_NAMES.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ORB_CACHE_POA_NAMES.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CACHE_POA_NAMES.getLocalName(), DEFAULT).set("off");
            // orb giop-minor-version attribute.
            node.get(REQUEST_PROPERTIES, ORB_GIOP_MINOR_VERSION.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.config.giop-minor-version"));
            node.get(REQUEST_PROPERTIES, ORB_GIOP_MINOR_VERSION.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, ORB_GIOP_MINOR_VERSION.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_GIOP_MINOR_VERSION.getLocalName(), DEFAULT).set(2);

            // describe the orb connection and naming sub-elements.
            node.get(CHILDREN, ORB_CONNECTION_CONFIG.getLocalName()).set(getORBConnectionConfigurationAdd(locale));
            node.get(CHILDREN, ORB_NAMING_CONFIG.getLocalName()).set(getORBNamingConfigurationAdd(locale));

            return node;
        }

        static ModelNode getORBConnectionConfigurationAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("orb.conn.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // connection retries attribute.
            node.get(REQUEST_PROPERTIES, ORB_CONN_RETRIES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.retries"));
            node.get(REQUEST_PROPERTIES, ORB_CONN_RETRIES.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, ORB_CONN_RETRIES.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CONN_RETRIES.getLocalName(), DEFAULT).set(5);
            // connection retry-interval attribute.
            node.get(REQUEST_PROPERTIES, ORB_CONN_RETRY_INTERVAL.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.retry-interval"));
            node.get(REQUEST_PROPERTIES, ORB_CONN_RETRY_INTERVAL.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, ORB_CONN_RETRY_INTERVAL.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CONN_RETRY_INTERVAL.getLocalName(), DEFAULT).set(500);
            // connection client-timeout attribute.
            node.get(REQUEST_PROPERTIES, ORB_CONN_CLIENT_TIMEOUT.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.client-timeout"));
            node.get(REQUEST_PROPERTIES, ORB_CONN_CLIENT_TIMEOUT.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, ORB_CONN_CLIENT_TIMEOUT.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CONN_CLIENT_TIMEOUT.getLocalName(), DEFAULT).set(0);
            // connection server-timeout attribute.
            node.get(REQUEST_PROPERTIES, ORB_CONN_SERVER_TIMEOUT.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.server-timeout"));
            node.get(REQUEST_PROPERTIES, ORB_CONN_SERVER_TIMEOUT.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, ORB_CONN_SERVER_TIMEOUT.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CONN_SERVER_TIMEOUT.getLocalName(), DEFAULT).set(0);
            // connection max-server-connections attribute.
            node.get(REQUEST_PROPERTIES, ORB_CONN_MAX_SERVER_CONNECTIONS.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.max-server-connections"));
            node.get(REQUEST_PROPERTIES, ORB_CONN_MAX_SERVER_CONNECTIONS.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, ORB_CONN_MAX_SERVER_CONNECTIONS.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CONN_MAX_SERVER_CONNECTIONS.getLocalName(), DEFAULT).set(Integer.MAX_VALUE);
            // connection max-managed-buf-size attribute.
            node.get(REQUEST_PROPERTIES, ORB_CONN_MAX_MANAGED_BUF_SIZE.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.max-managed-buf-size"));
            node.get(REQUEST_PROPERTIES, ORB_CONN_MAX_MANAGED_BUF_SIZE.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, ORB_CONN_MAX_MANAGED_BUF_SIZE.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CONN_MAX_MANAGED_BUF_SIZE.getLocalName(), DEFAULT).set(24);
            // connection outbuf-size attribute.
            node.get(REQUEST_PROPERTIES, ORB_CONN_OUTBUF_SIZE.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.outbuf-size"));
            node.get(REQUEST_PROPERTIES, ORB_CONN_OUTBUF_SIZE.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, ORB_CONN_OUTBUF_SIZE.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CONN_OUTBUF_SIZE.getLocalName(), DEFAULT).set(2048);
            // connection outbuf-cache-timeout attribute.
            node.get(REQUEST_PROPERTIES, ORB_CONN_OUTBUF_CACHE_TIMEOUT.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.conn.config.outbuf-cache-timeout"));
            node.get(REQUEST_PROPERTIES, ORB_CONN_OUTBUF_CACHE_TIMEOUT.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, ORB_CONN_OUTBUF_CACHE_TIMEOUT.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_CONN_OUTBUF_CACHE_TIMEOUT.getLocalName(), DEFAULT).set(-1);

            return node;
        }

        static ModelNode getORBNamingConfigurationAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("orb.naming.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // naming root-context attribute.
            node.get(REQUEST_PROPERTIES, ORB_NAMING_ROOT_CONTEXT.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.naming.config.root-context"));
            node.get(REQUEST_PROPERTIES, ORB_NAMING_ROOT_CONTEXT.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ORB_NAMING_ROOT_CONTEXT.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_NAMING_ROOT_CONTEXT.getLocalName(), DEFAULT).set("JBoss/Naming/root");
            // naming export-corbaloc attribute.
            node.get(REQUEST_PROPERTIES, ORB_NAMING_EXPORT_CORBALOC.getLocalName(), DESCRIPTION).
                    set(bundle.getString("orb.naming.config.export-corbaloc"));
            node.get(REQUEST_PROPERTIES, ORB_NAMING_EXPORT_CORBALOC.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ORB_NAMING_EXPORT_CORBALOC.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ORB_NAMING_EXPORT_CORBALOC.getLocalName(), DEFAULT).set("on");

            return node;
        }

        static ModelNode getPOAConfigurationAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("poa.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // poa monitoring attribute.
            node.get(REQUEST_PROPERTIES, POA_MONITORING.getLocalName(), DESCRIPTION).set(bundle.getString("poa.config.monitoring"));
            node.get(REQUEST_PROPERTIES, POA_MONITORING.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, POA_MONITORING.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, POA_MONITORING.getLocalName(), DEFAULT).set("off");
            // poa queue-wait attribute.
            node.get(REQUEST_PROPERTIES, POA_QUEUE_WAIT.getLocalName(), DESCRIPTION).set(bundle.getString("poa.config.queue-wait"));
            node.get(REQUEST_PROPERTIES, POA_QUEUE_WAIT.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, POA_QUEUE_WAIT.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, POA_QUEUE_WAIT.getLocalName(), DEFAULT).set("off");
            // poa queue-min attribute.
            node.get(REQUEST_PROPERTIES, POA_QUEUE_MIN.getLocalName(), DESCRIPTION).set(bundle.getString("poa.config.queue-min"));
            node.get(REQUEST_PROPERTIES, POA_QUEUE_MIN.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, POA_QUEUE_MIN.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, POA_QUEUE_MIN.getLocalName(), DEFAULT).set(10);
            // poa queue-max attribute.
            node.get(REQUEST_PROPERTIES, POA_QUEUE_MAX.getLocalName(), DESCRIPTION).set(bundle.getString("poa.config.queue-max"));
            node.get(REQUEST_PROPERTIES, POA_QUEUE_MAX.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, POA_QUEUE_MAX.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, POA_QUEUE_MAX.getLocalName(), DEFAULT).set(100);

            // describe the poa thread-pool sub-element.
            node.get(POA_REQUEST_PROC_CONFIG.getLocalName()).set(getPOAThreadPoolConfigurationAdd(locale));

            return node;
        }

        static ModelNode getPOAThreadPoolConfigurationAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("poa.request-processors.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // request-processors pool-size attribute.
            node.get(REQUEST_PROPERTIES, POA_REQUEST_PROC_POOL_SIZE.getLocalName(), DESCRIPTION).
                    set(bundle.getString("poa.request-processors.config.pool-size"));
            node.get(REQUEST_PROPERTIES, POA_REQUEST_PROC_POOL_SIZE.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, POA_REQUEST_PROC_POOL_SIZE.getLocalName(), REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, POA_REQUEST_PROC_POOL_SIZE.getLocalName(), DEFAULT).set(5);
            // request-processors max-threads attribute.
            node.get(REQUEST_PROPERTIES, POA_REQUEST_PROC_MAX_THREADS.getLocalName(), DESCRIPTION).
                    set(bundle.getString("poa.request-processors.config.max-threads"));
            node.get(REQUEST_PROPERTIES, POA_REQUEST_PROC_MAX_THREADS.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, POA_REQUEST_PROC_MAX_THREADS.getLocalName(), REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, POA_REQUEST_PROC_MAX_THREADS.getLocalName(), DEFAULT).set(20);

            return node;
        }

        static ModelNode getInteropConfigurationAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("interop.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // interop sun attribute.
            node.get(REQUEST_PROPERTIES, INTEROP_SUN.getLocalName(), DESCRIPTION).set(bundle.getString("interop.config.sun"));
            node.get(REQUEST_PROPERTIES, INTEROP_SUN.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, INTEROP_SUN.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, INTEROP_SUN.getLocalName(), DEFAULT).set("on");
            // interop comet attribute.
            node.get(REQUEST_PROPERTIES, INTEROP_COMET.getLocalName(), DESCRIPTION).set(bundle.getString("interop.config.comet"));
            node.get(REQUEST_PROPERTIES, INTEROP_COMET.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, INTEROP_COMET.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, INTEROP_COMET.getLocalName(), DEFAULT).set("off");
            // interop chunk-custom-rmi-valuetypes attribute.
            node.get(REQUEST_PROPERTIES, INTEROP_CHUNK_RMI_VALUETYPES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("interop.config.chunk-custom-rmi-valuetypes"));
            node.get(REQUEST_PROPERTIES, INTEROP_CHUNK_RMI_VALUETYPES.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, INTEROP_CHUNK_RMI_VALUETYPES.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, INTEROP_CHUNK_RMI_VALUETYPES.getLocalName(), DEFAULT).set("on");
            // interop lax-boolean-encoding attribute.
            node.get(REQUEST_PROPERTIES, INTEROP_LAX_BOOLEAN_ENCODING.getLocalName(), DESCRIPTION).
                    set(bundle.getString("interop.config.lax-boolean-encoding"));
            node.get(REQUEST_PROPERTIES, INTEROP_LAX_BOOLEAN_ENCODING.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, INTEROP_LAX_BOOLEAN_ENCODING.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, INTEROP_LAX_BOOLEAN_ENCODING.getLocalName(), DEFAULT).set("off");
            // interop indirection-encoding-disable attribute.
            node.get(REQUEST_PROPERTIES, INTEROP_INDIRECTION_ENCODING_DISABLE.getLocalName(), DESCRIPTION).
                    set(bundle.getString("interop.config.indirection-encoding-disable"));
            node.get(REQUEST_PROPERTIES, INTEROP_INDIRECTION_ENCODING_DISABLE.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, INTEROP_INDIRECTION_ENCODING_DISABLE.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, INTEROP_INDIRECTION_ENCODING_DISABLE.getLocalName(), DEFAULT).set("off");
            // interop strict-check-on-tc-creation attribute.
            node.get(REQUEST_PROPERTIES, INTEROP_STRICT_CHECK_ON_TC_CREATION.getLocalName(), DESCRIPTION).
                    set(bundle.getString("interop.config.strict-check-on-tc-creation"));
            node.get(REQUEST_PROPERTIES, INTEROP_STRICT_CHECK_ON_TC_CREATION.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, INTEROP_STRICT_CHECK_ON_TC_CREATION.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, INTEROP_STRICT_CHECK_ON_TC_CREATION.getLocalName(), DEFAULT).set("off");

            return node;
        }

        static ModelNode getSecurityConfigurationAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("security.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(1);

            // security support-ssl attribute.
            node.get(REQUEST_PROPERTIES, SECURITY_SUPPORT_SSL.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.support-ssl"));
            node.get(REQUEST_PROPERTIES, SECURITY_SUPPORT_SSL.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, SECURITY_SUPPORT_SSL.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SECURITY_SUPPORT_SSL.getLocalName(), DEFAULT).set("off");
            // security add-component-via-interceptor attribute.
            node.get(REQUEST_PROPERTIES, SECURITY_ADD_COMPONENT_INTERCEPTOR.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.add-component-via-interceptor"));
            node.get(REQUEST_PROPERTIES, SECURITY_ADD_COMPONENT_INTERCEPTOR.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, SECURITY_ADD_COMPONENT_INTERCEPTOR.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SECURITY_ADD_COMPONENT_INTERCEPTOR.getLocalName(), DEFAULT).set("on");
            // security client-supports attribute.
            node.get(REQUEST_PROPERTIES, SECURITY_CLIENT_SUPPORTS.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.client-supports"));
            node.get(REQUEST_PROPERTIES, SECURITY_CLIENT_SUPPORTS.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, SECURITY_CLIENT_SUPPORTS.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SECURITY_CLIENT_SUPPORTS.getLocalName(), DEFAULT).set(60);
            // security client-requires attribute.
            node.get(REQUEST_PROPERTIES, SECURITY_CLIENT_REQUIRES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.client-requires"));
            node.get(REQUEST_PROPERTIES, SECURITY_CLIENT_REQUIRES.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, SECURITY_CLIENT_REQUIRES.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SECURITY_CLIENT_REQUIRES.getLocalName(), DEFAULT).set(0);
            // security server-supports attribute.
            node.get(REQUEST_PROPERTIES, SECURITY_SERVER_SUPPORTS.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.server-supports"));
            node.get(REQUEST_PROPERTIES, SECURITY_SERVER_SUPPORTS.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, SECURITY_SERVER_SUPPORTS.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SECURITY_SERVER_SUPPORTS.getLocalName(), DEFAULT).set(60);
            // security server-requires attribute.
            node.get(REQUEST_PROPERTIES, SECURITY_SERVER_REQUIRES.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.server-requires"));
            node.get(REQUEST_PROPERTIES, SECURITY_SERVER_REQUIRES.getLocalName(), TYPE).set(ModelType.INT);
            node.get(REQUEST_PROPERTIES, SECURITY_SERVER_REQUIRES.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SECURITY_SERVER_REQUIRES.getLocalName(), DEFAULT).set(0);
            // security use-domain-socket-factory attribute.
            node.get(REQUEST_PROPERTIES, SECURITY_USE_DOMAIN_SF.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.use-domain-socket-factory"));
            node.get(REQUEST_PROPERTIES, SECURITY_USE_DOMAIN_SF.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, SECURITY_USE_DOMAIN_SF.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SECURITY_USE_DOMAIN_SF.getLocalName(), DEFAULT).set("off");
            // security use-domain-server-socket-factory attribute.
            node.get(REQUEST_PROPERTIES, SECURITY_USE_DOMAIN_SSF.getLocalName(), DESCRIPTION).
                    set(bundle.getString("security.config.use-domain-server-socket-factory"));
            node.get(REQUEST_PROPERTIES, SECURITY_USE_DOMAIN_SSF.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, SECURITY_USE_DOMAIN_SSF.getLocalName(), REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SECURITY_USE_DOMAIN_SSF.getLocalName(), DEFAULT).set("off");

            return node;
        }

        static ModelNode getPropertyConfigurationAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("property.config"));
            node.get(REQUIRED).set(false);
            node.get(MIN_OCCURS).set(0);
            node.get(MAX_OCCURS).set(Integer.MAX_VALUE);

            // property key and value attributes.
            node.get(REQUEST_PROPERTIES, PROP_KEY.getLocalName(), DESCRIPTION).set(bundle.getString("property.config.key"));
            node.get(REQUEST_PROPERTIES, PROP_KEY.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, PROP_KEY.getLocalName(), REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, PROP_VALUE.getLocalName(), DESCRIPTION).set(bundle.getString("property.config.value"));
            node.get(REQUEST_PROPERTIES, PROP_VALUE.getLocalName(), TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, PROP_VALUE.getLocalName(), REQUIRED).set(true);

            return node;
        }
    }
}
