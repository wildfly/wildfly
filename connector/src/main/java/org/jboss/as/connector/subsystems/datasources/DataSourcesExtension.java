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
package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOW_MULTIPLE_USERS;
import static org.jboss.as.connector.subsystems.datasources.Constants.AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECK_VALID_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MAJOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MINOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_XA_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENLISTMENT_TRACE;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JTA;
import static org.jboss.as.connector.subsystems.datasources.Constants.MCP;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE_SLOT;
import static org.jboss.as.connector.subsystems.datasources.Constants.NEW_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.datasources.Constants.NO_TX_SEPARATE_POOL;
import static org.jboss.as.connector.subsystems.datasources.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.datasources.Constants.PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.PREPARED_STATEMENTS_CACHE_SIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.QUERY_TIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.REAUTHPLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.REAUTH_PLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.datasources.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.SET_TX_QUERY_TIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.SHARE_PREPARED_STATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.SPY;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKING;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACK_STATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRANSACTION_ISOLATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_DELIMITER;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_PROPERTY;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_SELECTOR_STRATEGY_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_TRY_LOCK;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDATE_ON_MATCH;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.common.api.metadata.common.Capacity;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.Driver;
import org.jboss.jca.common.api.metadata.ds.DsPool;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author John Bailey
 */
public class DataSourcesExtension implements Extension {

    public static final String SUBSYSTEM_NAME = Constants.DATASOURCES;
    private static final String RESOURCE_NAME = DataSourcesExtension.class.getPackage().getName() + ".LocalDescriptions";

    static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(6, 0, 0);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, DataSourcesExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(final ExtensionContext context) {
        SUBSYSTEM_DATASOURCES_LOGGER.debugf("Initializing Datasources Extension");

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        // Register the remoting subsystem
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(DataSourcesSubsystemRootDefinition.createInstance(registerRuntimeOnly));


        subsystem.registerXMLElementWriter(new DataSourceSubsystemParser());


        if (registerRuntimeOnly) {
            subsystem.registerDeploymentModel(DataSourcesSubsystemRootDefinition.createDeployedInstance(registerRuntimeOnly));
        }
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DATASOURCES_1_1.getUriString(), DataSourceSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DATASOURCES_1_2.getUriString(), DataSourceSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DATASOURCES_2_0.getUriString(), DataSourceSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DATASOURCES_3_0.getUriString(), DataSourceSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DATASOURCES_4_0.getUriString(), DataSourceSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DATASOURCES_5_0.getUriString(), DataSourceSubsystemParser::new);
    }

    public static final class DataSourceSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {


        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();

            writer.writeStartElement(DATASOURCES);

            if (node.hasDefined(DATA_SOURCE) || node.hasDefined(XA_DATASOURCE)) {

                if (node.hasDefined(DATA_SOURCE)) {
                    writeDS(writer, false, node.get(DATA_SOURCE));
                }
                if (node.hasDefined(XA_DATASOURCE)) {
                    writeDS(writer, true, node.get(XA_DATASOURCE));
                }

            }

            if (node.hasDefined(JDBC_DRIVER_NAME)) {
                writer.writeStartElement(DataSources.Tag.DRIVERS.getLocalName());
                ModelNode drivers = node.get(JDBC_DRIVER_NAME);
                for (String driverName : drivers.keys()) {
                    ModelNode driver = drivers.get(driverName);
                    writer.writeStartElement(DataSources.Tag.DRIVER.getLocalName());
                    writer.writeAttribute(Driver.Attribute.NAME.getLocalName(), driver.require(DRIVER_NAME.getName()).asString());
                    if (has(driver, DRIVER_MODULE_NAME.getName())) {
                        String moduleName = driver.get(DRIVER_MODULE_NAME.getName()).asString();
                        if (has(driver, MODULE_SLOT.getName())) {
                            moduleName = moduleName + ":" + driver.get(MODULE_SLOT.getName()).asString();
                        }
                        writer.writeAttribute(Driver.Attribute.MODULE.getLocalName(), moduleName);
                    }
                    writeAttributeIfHas(writer, driver, Driver.Attribute.MAJOR_VERSION, DRIVER_MAJOR_VERSION.getName());
                    writeAttributeIfHas(writer, driver, Driver.Attribute.MINOR_VERSION, DRIVER_MINOR_VERSION.getName());
                    writeElementIfHas(writer, driver, Driver.Tag.DRIVER_CLASS.getLocalName(), DRIVER_CLASS_NAME.getName());
                    writeElementIfHas(writer, driver, Driver.Tag.XA_DATASOURCE_CLASS.getLocalName(), DRIVER_XA_DATASOURCE_CLASS_NAME.getName());
                    writeElementIfHas(writer, driver, Driver.Tag.DATASOURCE_CLASS.getLocalName(), DRIVER_DATASOURCE_CLASS_NAME.getName());

                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            writer.writeEndElement();
            writer.writeEndElement();
        }

        private void writeDS(XMLExtendedStreamWriter writer, boolean isXADataSource, ModelNode datasources) throws XMLStreamException {
            for (String dsName : datasources.keys()) {
                final ModelNode dataSourceNode = datasources.get(dsName);

                writer.writeStartElement(isXADataSource ? DataSources.Tag.XA_DATASOURCE.getLocalName()
                        : DataSources.Tag.DATASOURCE.getLocalName());
                JTA.marshallAsAttribute(dataSourceNode, writer);
                JNDI_NAME.marshallAsAttribute(dataSourceNode, writer);
                writer.writeAttribute("pool-name", dsName);
                ENABLED.marshallAsAttribute(dataSourceNode, writer);
                USE_JAVA_CONTEXT.marshallAsAttribute(dataSourceNode, writer);
                SPY.marshallAsAttribute(dataSourceNode, writer);
                USE_CCM.marshallAsAttribute(dataSourceNode, writer);
                CONNECTABLE.marshallAsAttribute(dataSourceNode, writer);
                TRACKING.marshallAsAttribute(dataSourceNode, writer);
                MCP.marshallAsAttribute(dataSourceNode, writer);
                ENLISTMENT_TRACE.marshallAsAttribute(dataSourceNode, writer);
                STATISTICS_ENABLED.marshallAsAttribute(dataSourceNode, writer);

                if (!isXADataSource) {
                    CONNECTION_URL.marshallAsElement(dataSourceNode, writer);
                    DRIVER_CLASS.marshallAsElement(dataSourceNode, writer);
                    DATASOURCE_CLASS.marshallAsElement(dataSourceNode, writer);
                    if (dataSourceNode.hasDefined(CONNECTION_PROPERTIES.getName())) {
                        for (Property connectionProperty : dataSourceNode.get(CONNECTION_PROPERTIES.getName()).asPropertyList()) {
                            writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                    .getValue().get("value").asString(), DataSource.Tag.CONNECTION_PROPERTY.getLocalName());
                        }
                    }
                }
                if (isXADataSource) {
                    if (dataSourceNode.hasDefined(XADATASOURCE_PROPERTIES.getName())) {
                        for (Property prop : dataSourceNode.get(XADATASOURCE_PROPERTIES.getName()).asPropertyList()) {
                            writeProperty(writer, dataSourceNode, prop.getName(), prop
                                    .getValue().get("value").asString(), XaDataSource.Tag.XA_DATASOURCE_PROPERTY.getLocalName());
                        }

                    }
                    XA_DATASOURCE_CLASS.marshallAsElement(dataSourceNode, writer);

                }
                DATASOURCE_DRIVER.marshallAsElement(dataSourceNode, writer);

                if (isXADataSource) {
                    URL_DELIMITER.marshallAsElement(dataSourceNode, writer);
                    URL_PROPERTY.marshallAsElement(dataSourceNode, writer);
                    URL_SELECTOR_STRATEGY_CLASS_NAME.marshallAsElement(dataSourceNode, writer);
                }
                NEW_CONNECTION_SQL.marshallAsElement(dataSourceNode, writer);
                TRANSACTION_ISOLATION.marshallAsElement(dataSourceNode, writer);

                if (!isXADataSource) {
                    URL_DELIMITER.marshallAsElement(dataSourceNode, writer);
                    URL_SELECTOR_STRATEGY_CLASS_NAME.marshallAsElement(dataSourceNode, writer);
                }
                boolean poolRequired = INITIAL_POOL_SIZE.isMarshallable(dataSourceNode) ||
                        MIN_POOL_SIZE.isMarshallable(dataSourceNode) ||
                        MAX_POOL_SIZE.isMarshallable(dataSourceNode) ||
                        POOL_PREFILL.isMarshallable(dataSourceNode) ||
                        POOL_FAIR.isMarshallable(dataSourceNode) ||
                        POOL_USE_STRICT_MIN.isMarshallable(dataSourceNode) ||
                        POOL_FLUSH_STRATEGY.isMarshallable(dataSourceNode) ||
                        ALLOW_MULTIPLE_USERS.isMarshallable(dataSourceNode) ||
                        CONNECTION_LISTENER_CLASS.isMarshallable(dataSourceNode) ||
                        CONNECTION_LISTENER_PROPERTIES.isMarshallable(dataSourceNode);
                if (isXADataSource) {
                    poolRequired = poolRequired
                            || SAME_RM_OVERRIDE.isMarshallable(dataSourceNode) ||
                            INTERLEAVING.isMarshallable(dataSourceNode) ||
                            NO_TX_SEPARATE_POOL.isMarshallable(dataSourceNode) ||
                            PAD_XID.isMarshallable(dataSourceNode) ||
                            WRAP_XA_RESOURCE.isMarshallable(dataSourceNode);
                }
                final boolean capacityRequired = CAPACITY_INCREMENTER_CLASS.isMarshallable(dataSourceNode) ||
                        CAPACITY_INCREMENTER_PROPERTIES.isMarshallable(dataSourceNode) ||
                        CAPACITY_DECREMENTER_CLASS.isMarshallable(dataSourceNode) ||
                        CAPACITY_DECREMENTER_PROPERTIES.isMarshallable(dataSourceNode);
                poolRequired = poolRequired || capacityRequired;
                if (poolRequired) {
                    writer.writeStartElement(isXADataSource ? XaDataSource.Tag.XA_POOL.getLocalName() : DataSource.Tag.POOL
                            .getLocalName());
                    MIN_POOL_SIZE.marshallAsElement(dataSourceNode, writer);
                    INITIAL_POOL_SIZE.marshallAsElement(dataSourceNode, writer);
                    MAX_POOL_SIZE.marshallAsElement(dataSourceNode, writer);
                    POOL_PREFILL.marshallAsElement(dataSourceNode, writer);
                    POOL_FAIR.marshallAsElement(dataSourceNode, writer);
                    POOL_USE_STRICT_MIN.marshallAsElement(dataSourceNode, writer);
                    POOL_FLUSH_STRATEGY.marshallAsElement(dataSourceNode, writer);
                    ALLOW_MULTIPLE_USERS.marshallAsElement(dataSourceNode, writer);

                    if (dataSourceNode.hasDefined(CONNECTION_LISTENER_CLASS.getName())) {
                        writer.writeStartElement(DsPool.Tag.CONNECTION_LISTENER.getLocalName());
                        writer.writeAttribute(org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(CONNECTION_LISTENER_CLASS.getName()).asString());

                        if (dataSourceNode.hasDefined(CONNECTION_LISTENER_PROPERTIES.getName())) {

                            for (Property connectionProperty : dataSourceNode.get(CONNECTION_LISTENER_PROPERTIES.getName())
                                    .asPropertyList()) {
                                writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                                .getValue().asString(),
                                        org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                .getLocalName());
                            }
                        }
                        writer.writeEndElement();
                    }


                    if (capacityRequired) {
                        writer.writeStartElement(DsPool.Tag.CAPACITY.getLocalName());
                        if (dataSourceNode.hasDefined(CAPACITY_INCREMENTER_CLASS.getName())) {
                            writer.writeStartElement(Capacity.Tag.INCREMENTER.getLocalName());
                            CAPACITY_INCREMENTER_CLASS.marshallAsAttribute(dataSourceNode, writer);
                            CAPACITY_INCREMENTER_PROPERTIES.marshallAsElement(dataSourceNode, writer);

                            writer.writeEndElement();
                        }
                        if (dataSourceNode.hasDefined(CAPACITY_DECREMENTER_CLASS.getName())) {
                            writer.writeStartElement(Capacity.Tag.DECREMENTER.getLocalName());
                            CAPACITY_DECREMENTER_CLASS.marshallAsAttribute(dataSourceNode, writer);
                            CAPACITY_DECREMENTER_PROPERTIES.marshallAsElement(dataSourceNode, writer);

                            writer.writeEndElement();
                        }
                        writer.writeEndElement();
                    }
                    if (isXADataSource) {
                        SAME_RM_OVERRIDE.marshallAsElement(dataSourceNode, writer);
                        INTERLEAVING.marshallAsElement(dataSourceNode, writer);
                        NO_TX_SEPARATE_POOL.marshallAsElement(dataSourceNode, writer);
                        PAD_XID.marshallAsElement(dataSourceNode, writer);
                        WRAP_XA_RESOURCE.marshallAsElement(dataSourceNode, writer);
                    }
                    writer.writeEndElement();
                }
                boolean securityRequired = USERNAME.isMarshallable(dataSourceNode) ||
                        PASSWORD.isMarshallable(dataSourceNode) ||
                        CREDENTIAL_REFERENCE.isMarshallable(dataSourceNode) ||
                        SECURITY_DOMAIN.isMarshallable(dataSourceNode) ||
                        ELYTRON_ENABLED.isMarshallable(dataSourceNode) ||
                        REAUTH_PLUGIN_CLASSNAME.isMarshallable(dataSourceNode) ||
                        REAUTHPLUGIN_PROPERTIES.isMarshallable(dataSourceNode);
                if (securityRequired) {
                    writer.writeStartElement(DataSource.Tag.SECURITY.getLocalName());
                    USERNAME.marshallAsElement(dataSourceNode, writer);
                    PASSWORD.marshallAsElement(dataSourceNode, writer);
                    SECURITY_DOMAIN.marshallAsElement(dataSourceNode, writer);
                    CREDENTIAL_REFERENCE.marshallAsElement(dataSourceNode, writer);
                    ELYTRON_ENABLED.marshallAsElement(dataSourceNode, writer);
                    AUTHENTICATION_CONTEXT.marshallAsElement(dataSourceNode, writer);

                    if (dataSourceNode.hasDefined(REAUTH_PLUGIN_CLASSNAME.getName())) {
                        writer.writeStartElement(DsSecurity.Tag.REAUTH_PLUGIN.getLocalName());
                        writer.writeAttribute(
                                org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(REAUTH_PLUGIN_CLASSNAME.getName()).asString());

                        if (dataSourceNode.hasDefined(REAUTHPLUGIN_PROPERTIES.getName())) {
                            for (Property connectionProperty : dataSourceNode.get(REAUTHPLUGIN_PROPERTIES.getName()).asPropertyList()) {
                                writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                                .getValue().asString(),
                                        org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                .getLocalName());
                            }
                        }
                        writer.writeEndElement();
                    }
                    writer.writeEndElement();
                }

                boolean recoveryRequired = RECOVERY_USERNAME.isMarshallable(dataSourceNode) ||
                        RECOVERY_PASSWORD.isMarshallable(dataSourceNode) ||
                        RECOVERY_SECURITY_DOMAIN.isMarshallable(dataSourceNode) ||
                        RECOVERY_ELYTRON_ENABLED.isMarshallable(dataSourceNode) ||
                        RECOVER_PLUGIN_CLASSNAME.isMarshallable(dataSourceNode) ||
                        RECOVERY_CREDENTIAL_REFERENCE.isMarshallable(dataSourceNode) ||
                        NO_RECOVERY.isMarshallable(dataSourceNode) ||
                        RECOVER_PLUGIN_PROPERTIES.isMarshallable(dataSourceNode);
                if (recoveryRequired && isXADataSource) {
                    writer.writeStartElement(XaDataSource.Tag.RECOVERY.getLocalName());
                    NO_RECOVERY.marshallAsAttribute(dataSourceNode, writer);
                    if (hasAnyOf(dataSourceNode, RECOVERY_USERNAME, RECOVERY_PASSWORD, RECOVERY_SECURITY_DOMAIN, RECOVERY_ELYTRON_ENABLED, RECOVERY_CREDENTIAL_REFERENCE)) {
                        writer.writeStartElement(Recovery.Tag.RECOVER_CREDENTIAL.getLocalName());
                        RECOVERY_USERNAME.marshallAsElement(dataSourceNode, writer);
                        RECOVERY_PASSWORD.marshallAsElement(dataSourceNode, writer);
                        RECOVERY_ELYTRON_ENABLED.marshallAsElement(dataSourceNode, writer);
                        RECOVERY_AUTHENTICATION_CONTEXT.marshallAsElement(dataSourceNode, writer);
                        RECOVERY_SECURITY_DOMAIN.marshallAsElement(dataSourceNode, writer);
                        RECOVERY_CREDENTIAL_REFERENCE.marshallAsElement(dataSourceNode, writer);
                        writer.writeEndElement();
                    }
                    if (hasAnyOf(dataSourceNode, RECOVER_PLUGIN_CLASSNAME)) {
                        writer.writeStartElement(Recovery.Tag.RECOVER_PLUGIN.getLocalName());
                        writer.writeAttribute(
                                org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(RECOVER_PLUGIN_CLASSNAME.getName()).asString());
                        if (dataSourceNode.hasDefined(RECOVER_PLUGIN_PROPERTIES.getName())) {
                            for (Property connectionProperty : dataSourceNode.get(RECOVER_PLUGIN_PROPERTIES.getName()).asPropertyList()) {
                                writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                                .getValue().asString(),
                                        org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                .getLocalName());
                            }
                        }
                        writer.writeEndElement();

                    }
                    writer.writeEndElement();
                }

                boolean validationRequired = VALID_CONNECTION_CHECKER_CLASSNAME.isMarshallable(dataSourceNode) ||

                        VALID_CONNECTION_CHECKER_PROPERTIES.isMarshallable(dataSourceNode) ||
                        CHECK_VALID_CONNECTION_SQL.isMarshallable(dataSourceNode) ||
                        VALIDATE_ON_MATCH.isMarshallable(dataSourceNode) ||
                        BACKGROUNDVALIDATION.isMarshallable(dataSourceNode) ||
                        BACKGROUNDVALIDATIONMILLIS.isMarshallable(dataSourceNode) ||
                        USE_FAST_FAIL.isMarshallable(dataSourceNode) ||
                        STALE_CONNECTION_CHECKER_CLASSNAME.isMarshallable(dataSourceNode) ||
                        STALE_CONNECTION_CHECKER_PROPERTIES.isMarshallable(dataSourceNode) ||
                        EXCEPTION_SORTER_CLASSNAME.isMarshallable(dataSourceNode) ||
                        EXCEPTION_SORTER_PROPERTIES.isMarshallable(dataSourceNode);
                if (validationRequired) {
                    writer.writeStartElement(DataSource.Tag.VALIDATION.getLocalName());
                    if (dataSourceNode.hasDefined(VALID_CONNECTION_CHECKER_CLASSNAME.getName())) {
                        writer.writeStartElement(Validation.Tag.VALID_CONNECTION_CHECKER.getLocalName());
                        writer.writeAttribute(
                                org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(VALID_CONNECTION_CHECKER_CLASSNAME.getName()).asString());

                        if (dataSourceNode.hasDefined(VALID_CONNECTION_CHECKER_PROPERTIES.getName())) {
                            for (Property connectionProperty : dataSourceNode.get(VALID_CONNECTION_CHECKER_PROPERTIES.getName())
                                    .asPropertyList()) {
                                writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                                .getValue().asString(),
                                        org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                .getLocalName());
                            }
                        }
                        writer.writeEndElement();
                    }
                    CHECK_VALID_CONNECTION_SQL.marshallAsElement(dataSourceNode, writer);
                    VALIDATE_ON_MATCH.marshallAsElement(dataSourceNode, writer);
                    BACKGROUNDVALIDATION.marshallAsElement(dataSourceNode, writer);
                    BACKGROUNDVALIDATIONMILLIS.marshallAsElement(dataSourceNode, writer);
                    USE_FAST_FAIL.marshallAsElement(dataSourceNode, writer);
                    if (dataSourceNode.hasDefined(STALE_CONNECTION_CHECKER_CLASSNAME.getName())) {
                        writer.writeStartElement(Validation.Tag.STALE_CONNECTION_CHECKER.getLocalName());
                        writer.writeAttribute(org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(STALE_CONNECTION_CHECKER_CLASSNAME.getName()).asString());

                        if (dataSourceNode.hasDefined(STALE_CONNECTION_CHECKER_PROPERTIES.getName())) {

                            for (Property connectionProperty : dataSourceNode.get(STALE_CONNECTION_CHECKER_PROPERTIES.getName())
                                    .asPropertyList()) {
                                writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                                .getValue().asString(),
                                        org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                .getLocalName());
                            }
                        }
                        writer.writeEndElement();
                    }
                    if (dataSourceNode.hasDefined(EXCEPTION_SORTER_CLASSNAME.getName())) {
                        writer.writeStartElement(Validation.Tag.EXCEPTION_SORTER.getLocalName());
                        writer.writeAttribute(
                                org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(EXCEPTION_SORTER_CLASSNAME.getName()).asString());
                        if (dataSourceNode.hasDefined(EXCEPTION_SORTER_PROPERTIES.getName())) {
                            for (Property connectionProperty : dataSourceNode.get(EXCEPTION_SORTER_PROPERTIES.getName())
                                    .asPropertyList()) {
                                writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                                .getValue().asString(),
                                        org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                .getLocalName());
                            }
                        }
                        writer.writeEndElement();
                    }
                    writer.writeEndElement();
                }
                boolean timeoutRequired = BLOCKING_TIMEOUT_WAIT_MILLIS.isMarshallable(dataSourceNode) ||
                        IDLETIMEOUTMINUTES.isMarshallable(dataSourceNode) ||
                        SET_TX_QUERY_TIMEOUT.isMarshallable(dataSourceNode) ||
                        QUERY_TIMEOUT.isMarshallable(dataSourceNode) ||
                        USE_TRY_LOCK.isMarshallable(dataSourceNode) ||
                        ALLOCATION_RETRY.isMarshallable(dataSourceNode) ||
                        ALLOCATION_RETRY_WAIT_MILLIS.isMarshallable(dataSourceNode) ||
                        XA_RESOURCE_TIMEOUT.isMarshallable(dataSourceNode);
                if (timeoutRequired) {
                    writer.writeStartElement(DataSource.Tag.TIMEOUT.getLocalName());
                    SET_TX_QUERY_TIMEOUT.marshallAsElement(dataSourceNode, writer);
                    BLOCKING_TIMEOUT_WAIT_MILLIS.marshallAsElement(dataSourceNode, writer);
                    IDLETIMEOUTMINUTES.marshallAsElement(dataSourceNode, writer);
                    QUERY_TIMEOUT.marshallAsElement(dataSourceNode, writer);
                    USE_TRY_LOCK.marshallAsElement(dataSourceNode, writer);
                    ALLOCATION_RETRY.marshallAsElement(dataSourceNode, writer);
                    ALLOCATION_RETRY_WAIT_MILLIS.marshallAsElement(dataSourceNode, writer);
                    XA_RESOURCE_TIMEOUT.marshallAsElement(dataSourceNode, writer);
                    writer.writeEndElement();
                }
                boolean statementRequired = hasAnyOf(dataSourceNode, TRACK_STATEMENTS, PREPARED_STATEMENTS_CACHE_SIZE, SHARE_PREPARED_STATEMENTS);
                if (statementRequired) {
                    writer.writeStartElement(DataSource.Tag.STATEMENT.getLocalName());
                    TRACK_STATEMENTS.marshallAsElement(dataSourceNode, writer);
                    PREPARED_STATEMENTS_CACHE_SIZE.marshallAsElement(dataSourceNode, writer);
                    SHARE_PREPARED_STATEMENTS.marshallAsElement(dataSourceNode, writer);

                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node,
                                         final Driver.Attribute attr, final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attr.getLocalName(), node.get(identifier).asString());
            }
        }

        private void writeProperty(XMLExtendedStreamWriter writer, ModelNode node, String name, String value, String localName)
                throws XMLStreamException {

            writer.writeStartElement(localName);
            writer.writeAttribute("name", name);
            writer.writeCharacters(value);
            writer.writeEndElement();

        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, String localName, String identifier)
                throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeStartElement(localName);
                String content = node.get(identifier).asString();
                if (content.indexOf('\n') > -1) {
                    writer.writeCharacters(content);
                } else {
                    // Use the method where staxmapper won't add new lines
                    char[] chars = content.toCharArray();
                    writer.writeCharacters(chars, 0, chars.length);
                }
                writer.writeEndElement();
            }
        }


        private boolean hasAnyOf(ModelNode node, SimpleAttributeDefinition... names) {
            for (SimpleAttributeDefinition current : names) {
                if (has(node, current.getName())) {
                    return true;
                }
            }
            return false;
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, DATASOURCES);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);

            list.add(subsystem);

            try {
                String localName = null;
                localName = reader.getLocalName();
                Element element = Element.forName(reader.getLocalName());
                SUBSYSTEM_DATASOURCES_LOGGER.tracef("%s -> %s", localName, element);
                switch (element) {
                    case SUBSYSTEM: {

                        final DsParser parser = new DsParser();
                        parser.parse(reader, list, address);
                        requireNoContent(reader);
                        break;
                    }
                }

            } catch (Exception e) {
                throw new XMLStreamException(e);
            }

        }

    }

}

