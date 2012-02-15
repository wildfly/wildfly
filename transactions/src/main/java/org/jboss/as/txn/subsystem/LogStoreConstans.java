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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * TODO class javadoc.
 *
 * @author @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a> 2011 Red Hat Inc.
 */

class LogStoreConstans {
    static final String PROBE = "probe";
    public static final String LOG_STORE = "log-store";
    public static final String TRANSACTIONS = "transactions";
    public static final String PARTECIPANTS = "partecipants";




    static enum PartecipatnStatus {
        Pending,
        Prepared,
        Failed,
        Heuristic,
        Readonly;

    }


    static SimpleAttributeDefinition LOG_STORE_TYPE = (new SimpleAttributeDefinitionBuilder("type", ModelType.STRING))
            .setAllowExpression(false)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode())
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    static SimpleAttributeDefinition TRANSACTION_AGE = (new SimpleAttributeDefinitionBuilder("age-in-seconds", ModelType.LONG))
                .setAllowExpression(false)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode())
                .setMeasurementUnit(MeasurementUnit.SECONDS)
                .build();

    static SimpleAttributeDefinition TRANSACTION_ID = (new SimpleAttributeDefinitionBuilder("id", ModelType.STRING))
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode())
                    .setMeasurementUnit(MeasurementUnit.NONE)
                    .build();

    static SimpleAttributeDefinition PARTECIPANT_STATUS = (new SimpleAttributeDefinitionBuilder("status", ModelType.STRING))
                        .setAllowExpression(false)
                        .setAllowNull(true)
                        .setDefaultValue(new ModelNode())
                        .setMeasurementUnit(MeasurementUnit.NONE)
                        .setValidator(new EnumValidator(PartecipatnStatus.class, false, false))
                        .build();

    static SimpleAttributeDefinition PARTECIPANT_JNDI_NAME = (new SimpleAttributeDefinitionBuilder("jndi-name", ModelType.STRING))
                        .setAllowExpression(false)
                        .setAllowNull(true)
                        .setDefaultValue(new ModelNode())
                        .setMeasurementUnit(MeasurementUnit.NONE)
                        .build();
}
