/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService.Derive;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.TimeUnitValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the strict-max-bean-pool resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StrictMaxPoolResourceDefinition extends SimpleResourceDefinition {

    public static final String STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME = "org.wildfly.ejb3.pool-config";
    public static final RuntimeCapability<Void> STRICT_MAX_POOL_CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, true, StrictMaxPoolConfigService.class).build();

    public static final SimpleAttributeDefinition MAX_POOL_SIZE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.MAX_POOL_SIZE, ModelType.INT, true)
                    .setDefaultValue(new ModelNode().set(StrictMaxPoolConfig.DEFAULT_MAX_POOL_SIZE))
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(1, Integer.MAX_VALUE, true, true))
                    .setAlternatives(EJB3SubsystemModel.DERIVE_SIZE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition DERIVE_SIZE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DERIVE_SIZE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    // DeriveSize.NONE is no longer legal but if presented we will correct to undefined
                    .setValidator(EnumValidator.create(DeriveSize.class, DeriveSize.LEGAL_VALUES))
                    .setCorrector(((newValue, currentValue) ->
                            (newValue.getType() == ModelType.STRING && DeriveSize.NONE.toString().equalsIgnoreCase(newValue.asString()))
                                    ? new ModelNode() : newValue))
                    .setAlternatives(EJB3SubsystemModel.MAX_POOL_SIZE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition INSTANCE_ACQUISITION_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.INSTANCE_ACQUISITION_TIMEOUT.getLocalName())
                    .setDefaultValue(new ModelNode().set(StrictMaxPoolConfig.DEFAULT_TIMEOUT))
                    .setAllowExpression(true)
                    .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition INSTANCE_ACQUISITION_TIMEOUT_UNIT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT_UNIT, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.INSTANCE_ACQUISITION_TIMEOUT_UNIT.getLocalName())
                    .setValidator(new TimeUnitValidator(true,true))
                    .setDefaultValue(new ModelNode().set(StrictMaxPoolConfig.DEFAULT_TIMEOUT_UNIT.name()))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setAllowExpression(true)
                    .build();
    public static final SimpleAttributeDefinition DERIVED_SIZE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DERIVED_SIZE, ModelType.INT, true)
                    .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { MAX_POOL_SIZE, DERIVE_SIZE, INSTANCE_ACQUISITION_TIMEOUT, INSTANCE_ACQUISITION_TIMEOUT_UNIT };
    private static final StrictMaxPoolAdd ADD_HANDLER = new StrictMaxPoolAdd();

    private static final String NONE_VALUE = "none";
    private static final String FROM_WORKER_POOLS_VALUE = "from-worker-pools";
    private static final String FROM_CPU_COUNT_VALUE = "from-cpu-count";

    enum DeriveSize {
        NONE(NONE_VALUE), FROM_WORKER_POOLS(FROM_WORKER_POOLS_VALUE), FROM_CPU_COUNT(FROM_CPU_COUNT_VALUE);

        // All values but NONE are 'legal' for use, although we provide a corrector to allow NONE as well
        // I use this convoluted mechanism to name these to make this more robust in case other values get added
        private static EnumSet<DeriveSize> LEGAL_VALUES = EnumSet.complementOf(EnumSet.of(NONE));
        private String value;

        DeriveSize(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        public static DeriveSize fromValue(String value) {
            switch (value) {
                case NONE_VALUE: return NONE;
                case FROM_WORKER_POOLS_VALUE: return FROM_WORKER_POOLS;
                case FROM_CPU_COUNT_VALUE: return FROM_CPU_COUNT;
                default:
                    return valueOf(value);
            }
        }
    }

    static Derive parseDeriveSize(OperationContext context, ModelNode strictMaxPoolModel) throws OperationFailedException {
        ModelNode dsNode = StrictMaxPoolResourceDefinition.DERIVE_SIZE.resolveModelAttribute(context, strictMaxPoolModel);
        if (dsNode.isDefined()) {
            DeriveSize deriveSize = DeriveSize.fromValue(dsNode.asString());

            switch (deriveSize) {
                case FROM_WORKER_POOLS:
                    return Derive.FROM_WORKER_POOLS;
                case FROM_CPU_COUNT:
                    return Derive.FROM_CPU_COUNT;
            }
        }
        return Derive.NONE;
    }

    StrictMaxPoolResourceDefinition() {
        super(new Parameters(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL))
                .setAddHandler(ADD_HANDLER)
                .setRemoveHandler(new ServiceRemoveStepHandler(null, ADD_HANDLER))
                .setAddRestartLevel(OperationEntry.Flag.RESTART_NONE)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setCapabilities(STRICT_MAX_POOL_CONFIG_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler osh = new StrictMaxPoolWriteHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, osh);
        }
        resourceRegistration.registerReadOnlyAttribute(DERIVED_SIZE, new StrictMaxPoolDerivedSizeReadHandler());
    }

}
