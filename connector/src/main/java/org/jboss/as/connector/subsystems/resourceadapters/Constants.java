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
package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.common.v10.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.v11.WorkManagerSecurity;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.v11.DataSource;
import org.jboss.jca.common.api.metadata.resourceadapter.v10.ResourceAdapter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;




/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class Constants {

    public static final String RESOURCEADAPTER_NAME = "resource-adapter";

    public static final String WORKMANAGER_NAME = "workmanager";

    public static final String DISTRIBUTED_WORKMANAGER_NAME = "distributed-workmanager";

    static final String RESOURCEADAPTERS_NAME = "resource-adapters";

    public static final String IRONJACAMAR_NAME = "ironjacamar";

    public static final String STATISTICS_NAME = "statistics";

    public static final String CONNECTIONDEFINITIONS_NAME = "connection-definitions";

    private static final String CLASS_NAME_NAME = "class-name";

    static final String POOL_NAME_NAME = "pool-name";

    private static final String USE_JAVA_CONTEXT_NAME = "use-java-context";

    private static final String ENABLED_NAME = "enabled";

    private static final String JNDINAME_NAME = "jndi-name";

    private static final String ALLOCATION_RETRY_NAME = "allocation-retry";

    private static final String ALLOCATION_RETRY_WAIT_MILLIS_NAME = "allocation-retry-wait-millis";

    private static final String XA_RESOURCE_TIMEOUT_NAME = "xa-resource-timeout";

    private static final String USETRYLOCK_NAME = "use-try-lock";

    private static final String SECURITY_DOMAIN_AND_APPLICATION_NAME = "security-domain-and-application";

    private static final String SECURITY_DOMAIN_NAME = "security-domain";

    private static final String APPLICATION_NAME = "security-application";

    private static final String USE_CCM_NAME = "use-ccm";

    private static final String SHARABLE_NAME = "sharable";

    private static final String ENLISTMENT_NAME = "enlistment";

    private static final String CONFIG_PROPERTIES_NAME = "config-properties";

    private static final String CONFIG_PROPERTY_VALUE_NAME = "value";

    private static final String ARCHIVE_NAME = "archive";

    private static final String MODULE_NAME = "module";

    private static final String BOOTSTRAPCONTEXT_NAME = "bootstrap-context";

    private static final String TRANSACTIONSUPPORT_NAME = "transaction-support";

    private static final String WM_SECURITY_NAME = "wm-security";

    private static final String WM_SECURITY_MAPPING_REQUIRED_NAME = "wm-security-mapping-required";

    private static final String WM_SECURITY_DOMAIN_NAME = "wm-security-domain";

    private static final String WM_SECURITY_DEFAULT_PRINCIPAL_NAME = "wm-security-default-principal";

    private static final String WM_SECURITY_DEFAULT_GROUP_NAME = "wm-security-default-group";

    private static final String WM_SECURITY_DEFAULT_GROUPS_NAME = "wm-security-default-groups";

    private static final String WM_SECURITY_MAPPING_FROM_NAME = "from";

    private static final String WM_SECURITY_MAPPING_TO_NAME = "to";

    private static final String WM_SECURITY_MAPPING_GROUP_NAME = "wm-security-mapping-group";

    private static final String WM_SECURITY_MAPPING_GROUPS_NAME = "wm-security-mapping-groups";

    private static final String WM_SECURITY_MAPPING_USER_NAME = "wm-security-mapping-user";

    private static final String WM_SECURITY_MAPPING_USERS_NAME = "wm-security-mapping-users";


    private static final String BEANVALIDATIONGROUPS_NAME = "beanvalidationgroups";

    static final String ADMIN_OBJECTS_NAME = "admin-objects";

    private static final String INTERLEAVING_NAME = "interleaving";

    private static final String NOTXSEPARATEPOOL_NAME = "no-tx-separate-pool";

    private static final String PAD_XID_NAME = "pad-xid";

    private static final String SAME_RM_OVERRIDE_NAME = "same-rm-override";

    private static final String WRAP_XA_RESOURCE_NAME = "wrap-xa-resource";

    private static final String RECOVERY_USERNAME_NAME = "recovery-username";

    private static final String RECOVERY_PASSWORD_NAME = "recovery-password";

    private static final String RECOVERY_SECURITY_DOMAIN_NAME = "recovery-security-domain";

    private static final String RECOVERLUGIN_CLASSNAME_NAME = "recovery-plugin-class-name";

    private static final String RECOVERLUGIN_PROPERTIES_NAME = "recovery-plugin-properties";

    private static final String NO_RECOVERY_NAME = "no-recovery";

    public static final String ACTIVATE = "activate";

    public static final String FLUSH_ALL_CONNECTION_IN_POOL = "flush-all-connection-in-pool";

    public static final String FLUSH_IDLE_CONNECTION_IN_POOL = "flush-idle-connection-in-pool";

    public static final String FLUSH_INVALID_CONNECTION_IN_POOL = "flush-invalid-connection-in-pool";

    public static final String FLUSH_GRACEFULLY_CONNECTION_IN_POOL = "flush-gracefully-connection-in-pool";

    public static final String TEST_CONNECTION_IN_POOL = "test-connection-in-pool";

    public static final String CLEAR_STATISTICS = "clear-statistics";


    static final SimpleAttributeDefinition CLASS_NAME = new SimpleAttributeDefinition(CLASS_NAME_NAME, CommonConnDef.Attribute.CLASS_NAME.getLocalName(), new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition JNDINAME = new SimpleAttributeDefinition(JNDINAME_NAME, CommonConnDef.Attribute.JNDI_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    public static final SimpleAttributeDefinition CONFIG_PROPERTIES = new SimpleAttributeDefinition(CONFIG_PROPERTIES_NAME, CommonConnDef.Tag.CONFIG_PROPERTY.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition CONFIG_PROPERTY_VALUE = new SimpleAttributeDefinition(CONFIG_PROPERTY_VALUE_NAME, CommonConnDef.Tag.CONFIG_PROPERTY.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition ARCHIVE = SimpleAttributeDefinitionBuilder.create(ARCHIVE_NAME, ModelType.STRING)
            .setXmlName(ResourceAdapter.Tag.ARCHIVE.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(false)
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setAttributeMarshaller(new AttributeMarshaller() {
                @Override
                public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                    if (resourceModel.hasDefined(attribute.getName())) {
                        writer.writeStartElement(attribute.getXmlName());
                        String archive = resourceModel.get(attribute.getName()).asString();
                        writer.writeCharacters(archive);
                        writer.writeEndElement();
                    }
                }
            })
            .setAlternatives(MODULE_NAME).build();

    static final SimpleAttributeDefinition MODULE = SimpleAttributeDefinitionBuilder.create(MODULE_NAME, ModelType.STRING)
            .setXmlName(AS7ResourceAdapterTags.MODULE.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(false)
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setAttributeMarshaller(new AttributeMarshaller() {
                @Override
                public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                    if (resourceModel.hasDefined(attribute.getName())) {
                        writer.writeStartElement(attribute.getXmlName());
                        String module = resourceModel.get(attribute.getName()).asString();
                        int separatorIndex = module.indexOf(":");
                        if (separatorIndex != -1) {
                            writer.writeAttribute("slot", module.substring(separatorIndex + 1));
                            module = module.substring(0, separatorIndex);

                        } else {
                            if (marshallDefault) {
                                writer.writeAttribute("slot", "main");
                            }
                        }
                        writer.writeAttribute("id", module);
                        writer.writeEndElement();
                    }
                }
            })
            .setAlternatives(ARCHIVE_NAME).build();

    static final SimpleAttributeDefinition BOOTSTRAP_CONTEXT = new SimpleAttributeDefinition(BOOTSTRAPCONTEXT_NAME, ResourceAdapter.Tag.BOOTSTRAP_CONTEXT.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition TRANSACTION_SUPPORT = new SimpleAttributeDefinition(TRANSACTIONSUPPORT_NAME, ResourceAdapter.Tag.TRANSACTION_SUPPORT.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE, new EnumValidator<TransactionSupportEnum>(TransactionSupportEnum.class, true, true));


    static final SimpleAttributeDefinition WM_SECURITY = new SimpleAttributeDefinitionBuilder(WM_SECURITY_NAME, ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    static final SimpleAttributeDefinition WM_SECURITY_MAPPING_REQUIRED = new SimpleAttributeDefinitionBuilder(WM_SECURITY_MAPPING_REQUIRED_NAME, ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .setXmlName(WorkManagerSecurity.Tag.MAPPING_REQUIRED.getLocalName())
            .build();

    static final SimpleAttributeDefinition WM_SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(WM_SECURITY_DOMAIN_NAME, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode("other"))
            .setXmlName(WorkManagerSecurity.Tag.DOMAIN.getLocalName())
            .build();

    static final SimpleAttributeDefinition WM_SECURITY_DEFAULT_PRINCIPAL = new SimpleAttributeDefinitionBuilder(WM_SECURITY_DEFAULT_PRINCIPAL_NAME, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setXmlName(WorkManagerSecurity.Tag.DEFAULT_PRINCIPAL.getLocalName())
            .build();

    static final StringListAttributeDefinition WM_SECURITY_DEFAULT_GROUPS = (new StringListAttributeDefinition.Builder(WM_SECURITY_DEFAULT_GROUPS_NAME))
            .setXmlName(WorkManagerSecurity.Tag.DEFAULT_GROUPS.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(true)
            .setElementValidator(new StringLengthValidator(1, false, true))
            .build();
    static final SimpleAttributeDefinition WM_SECURITY_DEFAULT_GROUP = new SimpleAttributeDefinition(WM_SECURITY_DEFAULT_GROUP_NAME, WorkManagerSecurity.Tag.GROUP.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);


    static final SimpleAttributeDefinition WM_SECURITY_MAPPING_FROM = new SimpleAttributeDefinitionBuilder(WM_SECURITY_MAPPING_FROM_NAME, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setXmlName(WorkManagerSecurity.Attribute.FROM.getLocalName())
            .build();

    static final SimpleAttributeDefinition WM_SECURITY_MAPPING_TO = new SimpleAttributeDefinitionBuilder(WM_SECURITY_MAPPING_TO_NAME, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setXmlName(WorkManagerSecurity.Attribute.TO.getLocalName())
            .build();

    static final ObjectTypeAttributeDefinition WM_SECURITY_MAPPING_GROUP = ObjectTypeAttributeDefinition.Builder.of(WM_SECURITY_MAPPING_GROUP_NAME, WM_SECURITY_MAPPING_FROM, WM_SECURITY_MAPPING_TO).build();
    static final ObjectListAttributeDefinition WM_SECURITY_MAPPING_GROUPS = ObjectListAttributeDefinition.Builder.of(WM_SECURITY_MAPPING_GROUPS_NAME, WM_SECURITY_MAPPING_GROUP)
            .setAllowNull(true)
            .build();

    static final ObjectTypeAttributeDefinition WM_SECURITY_MAPPING_USER = ObjectTypeAttributeDefinition.Builder.of(WM_SECURITY_MAPPING_USER_NAME, WM_SECURITY_MAPPING_FROM, WM_SECURITY_MAPPING_TO).build();
    static final ObjectListAttributeDefinition WM_SECURITY_MAPPING_USERS = ObjectListAttributeDefinition.Builder.of(WM_SECURITY_MAPPING_USERS_NAME, WM_SECURITY_MAPPING_USER)
            .setAllowNull(true)
            .build();

    static final StringListAttributeDefinition BEANVALIDATION_GROUPS = (new StringListAttributeDefinition.Builder(BEANVALIDATIONGROUPS_NAME))
            .setXmlName(ResourceAdapter.Tag.BEAN_VALIDATION_GROUP.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(true)
            .setElementValidator(new StringLengthValidator(1, false, true))
            .build();

    static final SimpleAttributeDefinition BEANVALIDATIONGROUP = new SimpleAttributeDefinition(BEANVALIDATIONGROUPS_NAME, ResourceAdapter.Tag.BEAN_VALIDATION_GROUP.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USE_JAVA_CONTEXT = new SimpleAttributeDefinition(USE_JAVA_CONTEXT_NAME, DataSource.Attribute.USE_JAVA_CONTEXT.getLocalName(), new ModelNode().set(Defaults.USE_JAVA_CONTEXT), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinition(ENABLED_NAME, DataSource.Attribute.ENABLED.getLocalName(), new ModelNode().set(Defaults.ENABLED), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(SECURITY_DOMAIN_NAME, ModelType.STRING)
            .setXmlName(CommonSecurity.Tag.SECURITY_DOMAIN.getLocalName())
            .setAllowExpression(true)
            .setAllowNull(true)
            .setAlternatives(SECURITY_DOMAIN_AND_APPLICATION_NAME, APPLICATION_NAME)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
            .addAccessConstraint(ResourceAdaptersExtension.RA_SECURITY_DEF)
            .build();


    static final SimpleAttributeDefinition SECURITY_DOMAIN_AND_APPLICATION = new SimpleAttributeDefinitionBuilder(SECURITY_DOMAIN_AND_APPLICATION_NAME, ModelType.STRING)
            .setXmlName(CommonSecurity.Tag.SECURITY_DOMAIN_AND_APPLICATION.getLocalName())
            .setAllowExpression(true)
            .setAllowNull(true)
            .setAlternatives(SECURITY_DOMAIN_NAME, APPLICATION_NAME)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
            .addAccessConstraint(ResourceAdaptersExtension.RA_SECURITY_DEF)
            .build();

    static final SimpleAttributeDefinition APPLICATION = new SimpleAttributeDefinitionBuilder(APPLICATION_NAME, ModelType.BOOLEAN)
            .setXmlName(CommonSecurity.Tag.APPLICATION.getLocalName())
            .setDefaultValue(new ModelNode(Defaults.APPLICATION_MANAGED_SECURITY))
            .setAllowExpression(true)
            .setAllowNull(true)
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setAlternatives(SECURITY_DOMAIN_NAME,SECURITY_DOMAIN_AND_APPLICATION_NAME)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
            .addAccessConstraint(ResourceAdaptersExtension.RA_SECURITY_DEF)
            .build();


    static SimpleAttributeDefinition ALLOCATION_RETRY = new SimpleAttributeDefinition(ALLOCATION_RETRY_NAME, TimeOut.Tag.ALLOCATION_RETRY.getLocalName(), new ModelNode(), ModelType.INT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ALLOCATION_RETRY_WAIT_MILLIS = new SimpleAttributeDefinition(ALLOCATION_RETRY_WAIT_MILLIS_NAME, TimeOut.Tag.ALLOCATION_RETRY_WAIT_MILLIS.getLocalName(), new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USETRYLOCK = new SimpleAttributeDefinition(USETRYLOCK_NAME, TimeOut.Tag.USE_TRY_LOCK.getLocalName(), new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USE_CCM = new SimpleAttributeDefinition(USE_CCM_NAME, DataSource.Attribute.USE_CCM.getLocalName(), new ModelNode().set(Defaults.USE_CCM), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SHARABLE = new SimpleAttributeDefinitionBuilder(SHARABLE_NAME, ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(Defaults.SHARABLE))
            .setXmlName(org.jboss.jca.common.api.metadata.common.v11.CommonConnDef.Attribute.SHARABLE.getLocalName())
            .build();

    static SimpleAttributeDefinition ENLISTMENT = new SimpleAttributeDefinitionBuilder(ENLISTMENT_NAME, ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(Defaults.ENLISTMENT))
            .setXmlName(org.jboss.jca.common.api.metadata.common.v11.CommonConnDef.Attribute.ENLISTMENT.getLocalName())
            .build();


    static SimpleAttributeDefinition INTERLEAVING = new SimpleAttributeDefinition(INTERLEAVING_NAME, CommonXaPool.Tag.INTERLEAVING.getLocalName(), new ModelNode().set(Defaults.INTERLEAVING), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition NOTXSEPARATEPOOL = new SimpleAttributeDefinition(NOTXSEPARATEPOOL_NAME, CommonXaPool.Tag.NO_TX_SEPARATE_POOLS.getLocalName(), new ModelNode().set(Defaults.NO_TX_SEPARATE_POOL), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition PAD_XID = new SimpleAttributeDefinition(PAD_XID_NAME, CommonXaPool.Tag.PAD_XID.getLocalName(), new ModelNode().set(Defaults.PAD_XID), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);


    static SimpleAttributeDefinition SAME_RM_OVERRIDE = new SimpleAttributeDefinitionBuilder(SAME_RM_OVERRIDE_NAME, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName(CommonXaPool.Tag.IS_SAME_RM_OVERRIDE.getLocalName())
            .build();

    static SimpleAttributeDefinition WRAP_XA_RESOURCE = new SimpleAttributeDefinition(WRAP_XA_RESOURCE_NAME, CommonXaPool.Tag.WRAP_XA_RESOURCE.getLocalName(), new ModelNode().set(Defaults.WRAP_XA_RESOURCE), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition XA_RESOURCE_TIMEOUT = new SimpleAttributeDefinition(XA_RESOURCE_TIMEOUT_NAME, TimeOut.Tag.XA_RESOURCE_TIMEOUT.getLocalName(), new ModelNode(), ModelType.INT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERY_USERNAME = new SimpleAttributeDefinitionBuilder(RECOVERY_USERNAME_NAME, ModelType.STRING, true)
            .setXmlName(Credential.Tag.USER_NAME.getLocalName())
            .setDefaultValue(new ModelNode())
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.NONE)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(ResourceAdaptersExtension.RA_SECURITY_DEF)
            .build();

    static SimpleAttributeDefinition RECOVERY_PASSWORD = new SimpleAttributeDefinitionBuilder(RECOVERY_PASSWORD_NAME, ModelType.STRING, true)
            .setXmlName(Credential.Tag.PASSWORD.getLocalName())
            .setDefaultValue(new ModelNode())
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.NONE)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(ResourceAdaptersExtension.RA_SECURITY_DEF)
            .build();

    static SimpleAttributeDefinition RECOVERY_SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(RECOVERY_SECURITY_DOMAIN_NAME, ModelType.STRING, true)
            .setXmlName(Credential.Tag.SECURITY_DOMAIN.getLocalName())
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setDefaultValue(new ModelNode())
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
            .addAccessConstraint(ResourceAdaptersExtension.RA_SECURITY_DEF)
            .build();

    static SimpleAttributeDefinition NO_RECOVERY = new SimpleAttributeDefinition(NO_RECOVERY_NAME, Recovery.Attribute.NO_RECOVERY.getLocalName(), new ModelNode(false), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERLUGIN_CLASSNAME = new SimpleAttributeDefinition(RECOVERLUGIN_CLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static PropertiesAttributeDefinition RECOVERLUGIN_PROPERTIES = new PropertiesAttributeDefinition.Builder(RECOVERLUGIN_PROPERTIES_NAME, true)
            .setAllowExpression(true)
            .setXmlName(org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName())
            .build();


    static final String WORK_ACTIVE_NAME = "work-active";

    static final String WORK_SUCEESSFUL_NAME = "work-successful";

    static final String WORK_FAILED_NAME = "work-failed";

    static final String DO_WORK_ACCEPTED_NAME = "dowork-accepted";

    static final String DO_WORK_REJECTED_NAME = "dowork-rejected";

    static final String SCHEDULED_WORK_ACCEPTED_NAME = "schedulework-accepted";

    static final String SCHEDULED_WORK_REJECTED_NAME = "schedulework-rejected";

    static final String START_WORK_ACCEPTED_NAME = "startwork-accepted";

    static final String START_WORK_REJECTED_NAME = "startwork-rejected";


    static SimpleAttributeDefinition WORK_ACTIVE = new SimpleAttributeDefinitionBuilder(WORK_ACTIVE_NAME, ModelType.INT)
            .setStorageRuntime()
            .build();

    static SimpleAttributeDefinition WORK_SUCCESSFUL = new SimpleAttributeDefinitionBuilder(WORK_SUCEESSFUL_NAME, ModelType.INT)
            .setStorageRuntime()
            .build();
    static SimpleAttributeDefinition WORK_FAILED = new SimpleAttributeDefinitionBuilder(WORK_FAILED_NAME, ModelType.INT)
            .setStorageRuntime()
            .build();

    static SimpleAttributeDefinition DO_WORK_ACCEPTED = new SimpleAttributeDefinitionBuilder(DO_WORK_ACCEPTED_NAME, ModelType.INT)
            .setStorageRuntime()
            .build();
    static SimpleAttributeDefinition DO_WORK_REJECTED = new SimpleAttributeDefinitionBuilder(DO_WORK_REJECTED_NAME, ModelType.INT)
            .setStorageRuntime()
            .build();

    static SimpleAttributeDefinition SCHEDULED_WORK_ACCEPTED = new SimpleAttributeDefinitionBuilder(SCHEDULED_WORK_ACCEPTED_NAME, ModelType.INT)
            .setStorageRuntime()
            .build();
    static SimpleAttributeDefinition SCHEDULED_WORK_REJECTED = new SimpleAttributeDefinitionBuilder(SCHEDULED_WORK_REJECTED_NAME, ModelType.INT)
            .setStorageRuntime()
            .build();

    static SimpleAttributeDefinition START_WORK_ACCEPTED = new SimpleAttributeDefinitionBuilder(START_WORK_ACCEPTED_NAME, ModelType.INT)
            .setStorageRuntime()
            .build();
    static SimpleAttributeDefinition START_WORK_REJECTED = new SimpleAttributeDefinitionBuilder(START_WORK_REJECTED_NAME, ModelType.INT)
            .setStorageRuntime()
            .build();


    public static SimpleAttributeDefinition[] WORKMANAGER_METRICS = new SimpleAttributeDefinition[]{WORK_ACTIVE, WORK_SUCCESSFUL, WORK_FAILED, DO_WORK_ACCEPTED,
            DO_WORK_REJECTED, SCHEDULED_WORK_ACCEPTED, SCHEDULED_WORK_REJECTED, START_WORK_ACCEPTED, START_WORK_REJECTED};

    public static final String WORKMANAGER_STATISTICS_ENABLED_NAME = "workmanager-statistics-enabled";
    public static SimpleAttributeDefinition WORKMANAGER_STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN)
            .setStorageRuntime()
            .build();
    public static SimpleAttributeDefinition WORKMANAGER_STATISTICS_ENABLED_DEPRECATED = new SimpleAttributeDefinitionBuilder(WORKMANAGER_STATISTICS_ENABLED_NAME, ModelType.BOOLEAN)
                .setStorageRuntime()
                .setDeprecated(ModelVersion.create(2))
                .build();

    public static final String DISTRIBUTED_STATISTICS_ENABLED_NAME = "distributed-workmanager-statistics-enabled";
    public static SimpleAttributeDefinition DISTRIBUTED_STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN)
            .setStorageRuntime()
            .build();
    public static SimpleAttributeDefinition DISTRIBUTED_STATISTICS_ENABLED_DEPRECATED = new SimpleAttributeDefinitionBuilder(DISTRIBUTED_STATISTICS_ENABLED_NAME, ModelType.BOOLEAN)
            .setStorageRuntime()
            .setDeprecated(ModelVersion.create(2))
            .build();

    public static final String DOWORK_DISTRIBUTION_ENABLED_NAME = "dowork-distribution-enabled";
    public static SimpleAttributeDefinition DOWORK_DISTRIBUTION_ENABLED = new SimpleAttributeDefinitionBuilder(DOWORK_DISTRIBUTION_ENABLED_NAME, ModelType.BOOLEAN)
            .setStorageRuntime()
            .build();
    public static final String SCHEDULEWORK_DISTRIBUTION_ENABLED_NAME = "schedulework-distribution-enabled";
    public static SimpleAttributeDefinition SCHEDULEWORK_DISTRIBUTION_ENABLED = new SimpleAttributeDefinitionBuilder(SCHEDULEWORK_DISTRIBUTION_ENABLED_NAME, ModelType.BOOLEAN)
            .setStorageRuntime()
            .build();
    public static final String STARTWORK_DISTRIBUTION_ENABLED_NAME = "startwork-distribution-enabled";
    public static SimpleAttributeDefinition STARTWORK_DISTRIBUTION_ENABLED = new SimpleAttributeDefinitionBuilder(STARTWORK_DISTRIBUTION_ENABLED_NAME, ModelType.BOOLEAN)
            .setStorageRuntime()
            .build();
    public static SimpleAttributeDefinition[] WORKMANAGER_RW_ATTRIBUTES = new SimpleAttributeDefinition[]{
            WORKMANAGER_STATISTICS_ENABLED,
            WORKMANAGER_STATISTICS_ENABLED_DEPRECATED
    };

    public static SimpleAttributeDefinition[] DISTRIBUTED_WORKMANAGER_RW_ATTRIBUTES = new SimpleAttributeDefinition[]{
            DISTRIBUTED_STATISTICS_ENABLED,
            DISTRIBUTED_STATISTICS_ENABLED_DEPRECATED,
            DOWORK_DISTRIBUTION_ENABLED,
            SCHEDULEWORK_DISTRIBUTION_ENABLED,
            STARTWORK_DISTRIBUTION_ENABLED
    };
}
