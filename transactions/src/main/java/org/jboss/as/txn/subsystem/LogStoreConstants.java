/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a> 2011 Red Hat Inc.
 */

class LogStoreConstants {
    static final String PROBE = "probe";
    static final String RECOVER = "recover";
    static final String DELETE = "delete";
    static final String REFRESH = "refresh";

    public static final String LOG_STORE = "log-store";
    public static final String TRANSACTIONS = "transactions";
    public static final String PARTICIPANTS = "participants";



    static enum ParticipantStatus {
        PENDING,
        PREPARED,
        FAILED,
        HEURISTIC,
        READONLY
    }

    static final String JMX_ON_ATTRIBUTE = "jmx-name";
    static final String JNDI_ATTRIBUTE = "jndi-name";
    static final String LOG_STORE_TYPE_ATTRIBUTE = "type";
    static final String EXPOSE_ALL_LOGS_ATTRIBUTE = "expose-all-logs";


    static final Map<String, String> MODEL_TO_JMX_TXN_NAMES =
            Collections.unmodifiableMap(new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
            {
                put(JMX_ON_ATTRIBUTE, null);
                put("id", "Id");
                put("age-in-seconds", "AgeInSeconds");
                put("type", "Type");
            }});

    static final Map<String, String> MODEL_TO_JMX_PARTICIPANT_NAMES =
            Collections.unmodifiableMap(new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
            {
                put(JMX_ON_ATTRIBUTE, null);
                put("type", "Type");
                put("status", "Status");
                put(JNDI_ATTRIBUTE, "JndiName");
                put("eis-product-name", "EisProductName");
                put("eis-product-version", "EisProductVersion");
            }});

    static final String[] TXN_JMX_NAMES = MODEL_TO_JMX_TXN_NAMES.values().toArray(new String[MODEL_TO_JMX_TXN_NAMES.size()]);
    static final String[] PARTICIPANT_JMX_NAMES = MODEL_TO_JMX_PARTICIPANT_NAMES.values().toArray(new String[MODEL_TO_JMX_PARTICIPANT_NAMES.size()]);

    static SimpleAttributeDefinition LOG_STORE_TYPE = (new SimpleAttributeDefinitionBuilder(LOG_STORE_TYPE_ATTRIBUTE, ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode("default"))
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    static final SimpleAttributeDefinition EXPOSE_ALL_LOGS = new SimpleAttributeDefinitionBuilder(EXPOSE_ALL_LOGS_ATTRIBUTE, ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(false))
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setStorageRuntime()
            .build();

    static SimpleAttributeDefinition JMX_NAME = (new SimpleAttributeDefinitionBuilder(JMX_ON_ATTRIBUTE, ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setValidator(new StringLengthValidator(0, true))
            .build();

    static SimpleAttributeDefinition TRANSACTION_AGE = (new SimpleAttributeDefinitionBuilder("age-in-seconds", ModelType.LONG))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .build();

    static SimpleAttributeDefinition TRANSACTION_ID = (new SimpleAttributeDefinitionBuilder("id", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    static SimpleAttributeDefinition PARTICIPANT_STATUS = (new SimpleAttributeDefinitionBuilder("status", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setValidator(new EnumValidator<ParticipantStatus>(ParticipantStatus.class, true, false))
            .build();

    static SimpleAttributeDefinition PARTICIPANT_JNDI_NAME = (new SimpleAttributeDefinitionBuilder(JNDI_ATTRIBUTE, ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    static SimpleAttributeDefinition EIS_NAME = (new SimpleAttributeDefinitionBuilder("eis-product-name", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setValidator(new StringLengthValidator(0, true))
            .build();

    static SimpleAttributeDefinition EIS_VERSION = (new SimpleAttributeDefinitionBuilder("eis-product-version", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setValidator(new StringLengthValidator(0, true))
            .build();

    static SimpleAttributeDefinition RECORD_TYPE = (new SimpleAttributeDefinitionBuilder("type", ModelType.STRING))
            .setAllowExpression(false)
            .setRequired(false)
            .setDefaultValue(new ModelNode())
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    static String jmxNameToModelName(Map<String, String> map, String jmxName) {
        for(Map.Entry<String, String> e : map.entrySet()) {
            if (jmxName.equals(e.getValue()))
                return e.getKey();
        }

        return null;

    }
}


