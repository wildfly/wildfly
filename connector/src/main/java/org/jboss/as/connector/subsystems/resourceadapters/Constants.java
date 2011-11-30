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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.CommonIronJacamar;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.Driver;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;


/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class Constants {

    static final String RESOURCEADAPTER_NAME = "resource-adapter";

    static final String RESOURCEADAPTERS_NAME = "resource-adapters";

    static final String CONNECTIONDEFINITIONS_NAME = "connection-definitions";

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

    private static final String CONFIG_PROPERTIES_NAME = "config-properties";

    private static final String CONFIG_PROPERTY_VALUE_NAME = "value";

    private static final String ARCHIVE_NAME = "archive";

    private static final String BOOTSTRAPCONTEXT_NAME = "bootstrapcontext";

    private static final String TRANSACTIONSUPPORT_NAME = "transaction-support";

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

    private static final String RECOVERLUGIN_CLASSNAME_NAME = "recovery-plugin-properties";

    private static final String RECOVERLUGIN_PROPERTIES_NAME = "recovery-plugin-properties";

    private static final String NO_RECOVERY_NAME = "no-recovery";


    static final SimpleAttributeDefinition CLASS_NAME = new SimpleAttributeDefinition(CLASS_NAME_NAME, CommonConnDef.Attribute.CLASS_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition JNDINAME = new SimpleAttributeDefinition(JNDINAME_NAME, CommonConnDef.Attribute.JNDI_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition CONFIG_PROPERTIES = new SimpleAttributeDefinition(CONFIG_PROPERTIES_NAME, CommonConnDef.Tag.CONFIG_PROPERTY.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition CONFIG_PROPERTY_VALUE = new SimpleAttributeDefinition(CONFIG_PROPERTY_VALUE_NAME, CommonConnDef.Tag.CONFIG_PROPERTY.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition ARCHIVE = new SimpleAttributeDefinition(ARCHIVE_NAME, ResourceAdapter.Tag.ARCHIVE.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition BOOTSTRAPCONTEXT = new SimpleAttributeDefinition(BOOTSTRAPCONTEXT_NAME, ResourceAdapter.Tag.BOOTSTRAP_CONTEXT.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition TRANSACTIONSUPPORT = new SimpleAttributeDefinition(TRANSACTIONSUPPORT_NAME, ResourceAdapter.Tag.TRANSACTION_SUPPORT.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition BEANVALIDATIONGROUPS = new SimpleAttributeDefinition(BEANVALIDATIONGROUPS_NAME, ResourceAdapter.Tag.BEAN_VALIDATION_GROUP.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USE_JAVA_CONTEXT = new SimpleAttributeDefinition(USE_JAVA_CONTEXT_NAME, DataSource.Attribute.USE_JAVA_CONTEXT.getLocalName(), new ModelNode().set(Defaults.USE_JAVA_CONTEXT), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinition(ENABLED_NAME, DataSource.Attribute.ENABLED.getLocalName(), new ModelNode().set(Defaults.ENABLED), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinition(SECURITY_DOMAIN_NAME, CommonSecurity.Tag.SECURITY_DOMAIN.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition SECURITY_DOMAIN_AND_APPLICATION = new SimpleAttributeDefinition(SECURITY_DOMAIN_AND_APPLICATION_NAME, CommonSecurity.Tag.SECURITY_DOMAIN_AND_APPLICATION.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition APPLICATION = new SimpleAttributeDefinition(APPLICATION_NAME, CommonSecurity.Tag.APPLICATION.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ALLOCATION_RETRY = new SimpleAttributeDefinition(ALLOCATION_RETRY_NAME, TimeOut.Tag.ALLOCATION_RETRY.getLocalName(),  new ModelNode(), ModelType.INT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ALLOCATION_RETRY_WAIT_MILLIS = new SimpleAttributeDefinition(ALLOCATION_RETRY_WAIT_MILLIS_NAME, TimeOut.Tag.ALLOCATION_RETRY_WAIT_MILLIS.getLocalName(),  new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USETRYLOCK = new SimpleAttributeDefinition(USETRYLOCK_NAME, TimeOut.Tag.USE_TRY_LOCK.getLocalName(),  new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USE_CCM = new SimpleAttributeDefinition(USE_CCM_NAME, DataSource.Attribute.USE_CCM.getLocalName(), new ModelNode().set(Defaults.USE_CCM), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition INTERLEAVING = new SimpleAttributeDefinition(INTERLEAVING_NAME, CommonXaPool.Tag.INTERLEAVING.getLocalName(), new ModelNode().set(Defaults.INTERLEAVING), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition NOTXSEPARATEPOOL = new SimpleAttributeDefinition(NOTXSEPARATEPOOL_NAME, CommonXaPool.Tag.NO_TX_SEPARATE_POOLS.getLocalName(), new ModelNode().set(Defaults.NO_TX_SEPARATE_POOL), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition PAD_XID = new SimpleAttributeDefinition(PAD_XID_NAME, CommonXaPool.Tag.PAD_XID.getLocalName(), new ModelNode().set(Defaults.PAD_XID), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SAME_RM_OVERRIDE = new SimpleAttributeDefinition(SAME_RM_OVERRIDE_NAME, CommonXaPool.Tag.IS_SAME_RM_OVERRIDE.getLocalName(), new ModelNode(), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition WRAP_XA_RESOURCE = new SimpleAttributeDefinition(WRAP_XA_RESOURCE_NAME, CommonXaPool.Tag.WRAP_XA_RESOURCE.getLocalName(), new ModelNode().set(Defaults.WRAP_XA_RESOURCE), ModelType.BOOLEAN, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition XA_RESOURCE_TIMEOUT = new SimpleAttributeDefinition(XA_RESOURCE_TIMEOUT_NAME, TimeOut.Tag.XA_RESOURCE_TIMEOUT.getLocalName(),  new ModelNode(), ModelType.INT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERY_USERNAME = new SimpleAttributeDefinition(RECOVERY_USERNAME_NAME, Recovery.Tag.RECOVER_CREDENTIAL.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERY_PASSWORD = new SimpleAttributeDefinition(RECOVERY_PASSWORD_NAME, Recovery.Tag.RECOVER_CREDENTIAL.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERY_SECURITY_DOMAIN = new SimpleAttributeDefinition(RECOVERY_SECURITY_DOMAIN_NAME, Recovery.Tag.RECOVER_CREDENTIAL.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition NO_RECOVERY = new SimpleAttributeDefinition(NO_RECOVERY_NAME, Recovery.Attribute.NO_RECOVERY.getLocalName(),  new ModelNode(), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERLUGIN_CLASSNAME = new SimpleAttributeDefinition(RECOVERLUGIN_CLASSNAME_NAME, Recovery.Tag.RECOVER_PLUGIN.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERLUGIN_PROPERTIES = new SimpleAttributeDefinition(RECOVERLUGIN_PROPERTIES_NAME, Recovery.Tag.RECOVER_PLUGIN.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);


}
