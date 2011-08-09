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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PoolConfigService;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT_UNIT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_POOL_SIZE;

/**
 * Adds a strict-max-pool to the EJB3 subsystem's bean-instance-pools. The {#performRuntime runtime action}
 * will create and install a {@link PoolConfigService}
 * <p/>
 * User: Jaikiran Pai
 */
public class StrictMaxPoolAdd extends AbstractAddStepHandler {

    public static final StrictMaxPoolAdd INSTANCE = new StrictMaxPoolAdd();

    /**
     * Description provider for the strict-max-pool add operation
     */
    public static DescriptionProvider STRICT_MAX_POOL_ADD_DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            // TODO: Use the Locale
            final ModelNode description = new ModelNode();
            // set the description of this operation
            description.get(ModelDescriptionConstants.DESCRIPTION).set("Adds a bean instance pool which has a strict upper limit for bean instances");

            // setup the "max-pool-size" param description
            description.get(REQUEST_PROPERTIES, MAX_POOL_SIZE, DESCRIPTION).set("The maximum number of bean instances that " +
                    "this pool can hold at a given point in time");
            description.get(REQUEST_PROPERTIES, MAX_POOL_SIZE, TYPE).set(ModelType.INT);
            description.get(REQUEST_PROPERTIES, MAX_POOL_SIZE, REQUIRED).set(false);
            description.get(REQUEST_PROPERTIES, MAX_POOL_SIZE, DEFAULT).set(StrictMaxPoolConfig.DEFAULT_MAX_POOL_SIZE);

            // setup the "timeout" param description
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT, DESCRIPTION).set("The maximum amount of time to wait for a bean instance to be available from the pool");
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT, TYPE).set(ModelType.INT);
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT, REQUIRED).set(false);
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT, DEFAULT).set(StrictMaxPoolConfig.DEFAULT_TIMEOUT);

            // setup the "timeout-unit" param description
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, DESCRIPTION).set("The instance acquisition timeout unit.");
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, TYPE).set(ModelType.STRING);
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, REQUIRED).set(false);
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, DEFAULT).set(StrictMaxPoolConfig.DEFAULT_TIMEOUT_UNIT.name());
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.HOURS.name());
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.MINUTES.name());
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.SECONDS.name());
            description.get(REQUEST_PROPERTIES, INSTANCE_ACQUISITION_TIMEOUT_UNIT, ALLOWED).set(TimeUnit.MILLISECONDS.name());

            return description;
        }
    };


    /**
     * Populate the <code>strictMaxPoolModel</code> from the <code>operation</code>
     *
     * @param operation          the operation
     * @param strictMaxPoolModel strict-max-pool ModelNode
     * @throws OperationFailedException
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode strictMaxPoolModel) throws OperationFailedException {
        final String poolName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        strictMaxPoolModel.get(EJB3SubsystemModel.NAME).set(poolName);
        // max-pool-size
        int maxPoolSize = StrictMaxPoolConfig.DEFAULT_MAX_POOL_SIZE;
        if (operation.hasDefined(EJB3SubsystemModel.MAX_POOL_SIZE)) {
            maxPoolSize = operation.get(EJB3SubsystemModel.MAX_POOL_SIZE).asInt();
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("Invalid value: " + maxPoolSize + " for " + EJB3SubsystemModel.MAX_POOL_SIZE);
            }
        }
        strictMaxPoolModel.get(EJB3SubsystemModel.MAX_POOL_SIZE).set(maxPoolSize);


        // instance-acquisition-timeout
        long instanceAcquisitionTimeout = StrictMaxPoolConfig.DEFAULT_TIMEOUT;
        if (operation.hasDefined(INSTANCE_ACQUISITION_TIMEOUT)) {
            instanceAcquisitionTimeout = operation.get(INSTANCE_ACQUISITION_TIMEOUT).asLong();
            if (instanceAcquisitionTimeout <= 0) {
                throw new IllegalArgumentException("Invalid value: " + instanceAcquisitionTimeout + " for " + INSTANCE_ACQUISITION_TIMEOUT);
            }
        }
        strictMaxPoolModel.get(INSTANCE_ACQUISITION_TIMEOUT).set(instanceAcquisitionTimeout);

        // instance-acquisition-timeout-unit
        String instanceAcquisitionTimeoutUnit = StrictMaxPoolConfig.DEFAULT_TIMEOUT_UNIT.name();
        if (operation.hasDefined(INSTANCE_ACQUISITION_TIMEOUT_UNIT)) {
            instanceAcquisitionTimeoutUnit = operation.get(INSTANCE_ACQUISITION_TIMEOUT_UNIT).asString();
            if (!this.isValidTimeoutUnit(instanceAcquisitionTimeoutUnit)) {
                throw new IllegalArgumentException("Invalid value: " + instanceAcquisitionTimeoutUnit + " for " + INSTANCE_ACQUISITION_TIMEOUT_UNIT);
            }
        }
        strictMaxPoolModel.get(INSTANCE_ACQUISITION_TIMEOUT_UNIT).set(instanceAcquisitionTimeoutUnit.trim().toUpperCase(Locale.ENGLISH));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode strictMaxPoolModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> serviceControllers) throws OperationFailedException {

        final String poolName = strictMaxPoolModel.require(EJB3SubsystemModel.NAME).asString();
        final int maxPoolSize = strictMaxPoolModel.get(EJB3SubsystemModel.MAX_POOL_SIZE).asInt();
        final long timeout = strictMaxPoolModel.get(EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT).asLong();
        final String unit = strictMaxPoolModel.get(EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT_UNIT).asString();
        // create the pool config
        final PoolConfig strictMaxPoolConfig = new StrictMaxPoolConfig(poolName, maxPoolSize, timeout, TimeUnit.valueOf(unit));
        // create and install the service
        final PoolConfigService poolConfigService = new PoolConfigService(strictMaxPoolConfig);
        final ServiceName serviceName = PoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName);
        final ServiceController serviceController = context.getServiceTarget().addService(serviceName, poolConfigService).install();
        // add this to the service controllers
        serviceControllers.add(serviceController);

    }

    private boolean isValidTimeoutUnit(final String val) {
        if (val == null || val.trim().isEmpty()) {
            return false;
        }
        final String upperCaseUnitValue = val.toUpperCase(Locale.ENGLISH);
        try {
            final TimeUnit unit = TimeUnit.valueOf(upperCaseUnitValue);
            if (unit == TimeUnit.SECONDS || unit == TimeUnit.HOURS || unit == TimeUnit.MINUTES || unit == TimeUnit.MILLISECONDS) {
                return true;
            }
        } catch (IllegalArgumentException iae) {
            return false;
        }
        return false;
    }

}
