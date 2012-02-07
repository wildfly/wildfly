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

package org.jboss.as.connector.pool;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class Constants {

    private static final String MIN_POOL_SIZE_NAME = "min-pool-size";

    private static final String MAX_POOL_SIZE_NAME = "max-pool-size";

    private static final String POOL_PREFILL_NAME = "pool-prefill";

    private static final String POOL_USE_STRICT_MIN_NAME = "pool-use-strict-min";

    private static final String BACKGROUNDVALIDATIONMILLIS_NAME = "background-validation-millis";

    private static final String BACKGROUNDVALIDATION_NAME = "background-validation";

    private static final String USE_FAST_FAIL_NAME = "use-fast-fail";

    private static final String BLOCKING_TIMEOUT_WAIT_MILLIS_NAME = "blocking-timeout-wait-millis";

    private static final String IDLETIMEOUTMINUTES_NAME = "idle-timeout-minutes";

    private static final String FLUSH_STRATEGY_NAME = "flush-strategy";


    public static final SimpleAttributeDefinition BLOCKING_TIMEOUT_WAIT_MILLIS = new SimpleAttributeDefinition(BLOCKING_TIMEOUT_WAIT_MILLIS_NAME, TimeOut.Tag.BLOCKING_TIMEOUT_MILLIS.getLocalName(),  new ModelNode(), ModelType.LONG, true , true, MeasurementUnit.MILLISECONDS);

    public static final SimpleAttributeDefinition IDLETIMEOUTMINUTES = new SimpleAttributeDefinition(IDLETIMEOUTMINUTES_NAME, TimeOut.Tag.IDLE_TIMEOUT_MINUTES.getLocalName(),  new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.MINUTES);

    public static final SimpleAttributeDefinition BACKGROUNDVALIDATIONMILLIS = new SimpleAttributeDefinition(BACKGROUNDVALIDATIONMILLIS_NAME, Validation.Tag.BACKGROUND_VALIDATION_MILLIS.getLocalName(),  new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.MILLISECONDS);

    public static final SimpleAttributeDefinition BACKGROUNDVALIDATION = new SimpleAttributeDefinition(BACKGROUNDVALIDATION_NAME, Validation.Tag.BACKGROUND_VALIDATION.getLocalName(), new ModelNode().set(Defaults.BACKGROUND_VALIDATION), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    public static final SimpleAttributeDefinition USE_FAST_FAIL = new SimpleAttributeDefinition(USE_FAST_FAIL_NAME, Validation.Tag.USE_FAST_FAIL.getLocalName(), new ModelNode().set(Defaults.USE_FAST_FAIl), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    public static final SimpleAttributeDefinition MAX_POOL_SIZE = new SimpleAttributeDefinition(MAX_POOL_SIZE_NAME, CommonPool.Tag.MAX_POOL_SIZE.getLocalName(), new ModelNode().set(Defaults.MAX_POOL_SIZE), ModelType.INT, true, true, MeasurementUnit.NONE);

    public static final SimpleAttributeDefinition MIN_POOL_SIZE = new SimpleAttributeDefinition(MIN_POOL_SIZE_NAME, CommonPool.Tag.MIN_POOL_SIZE.getLocalName(), new ModelNode().set(Defaults.MIN_POOL_SIZE), ModelType.INT, true, true, MeasurementUnit.NONE);

    public static final SimpleAttributeDefinition POOL_PREFILL = new SimpleAttributeDefinition(POOL_PREFILL_NAME, CommonPool.Tag.PREFILL.getLocalName(), new ModelNode().set(Defaults.PREFILL), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    public static final SimpleAttributeDefinition POOL_USE_STRICT_MIN = new SimpleAttributeDefinition(POOL_USE_STRICT_MIN_NAME, CommonPool.Tag.USE_STRICT_MIN.getLocalName(), new ModelNode().set(Defaults.USE_STRICT_MIN), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    public static SimpleAttributeDefinition POOL_FLUSH_STRATEGY = new SimpleAttributeDefinition(FLUSH_STRATEGY_NAME, CommonPool.Tag.FLUSH_STRATEGY.getLocalName(), new ModelNode().set(Defaults.FLUSH_STRATEGY.getName()), ModelType.STRING, true, true, MeasurementUnit.NONE, new ParameterValidator() {
            @Override
            public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
                String str = value.asString();

                if ( FlushStrategy.forName(str) == FlushStrategy.UNKNOWN) {
                    throw new OperationFailedException(new ModelNode().set("Unknown FlushStrategy"));
                }
            }
            }

            @Override
            public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
                validateParameter(parameterName, value.resolve());
            }
        });;


}
