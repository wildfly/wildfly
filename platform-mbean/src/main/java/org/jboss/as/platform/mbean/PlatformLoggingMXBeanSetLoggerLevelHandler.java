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

package org.jboss.as.platform.mbean;

import java.lang.management.ManagementFactory;
import java.util.Locale;

import javax.management.JMException;
import javax.management.JMRuntimeException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Executes the {@code java.lang.management.PlatformLoggingMXBean.setLoggerLevel(String loggerName, String levelName)} method.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PlatformLoggingMXBeanSetLoggerLevelHandler implements OperationStepHandler, DescriptionProvider {

    public static final PlatformLoggingMXBeanSetLoggerLevelHandler INSTANCE = new PlatformLoggingMXBeanSetLoggerLevelHandler();

    private static final String[] SIGNATURE = { String.class.getName(), String.class.getName() };

    private final ParametersValidator parametersValidator = new ParametersValidator();

    private PlatformLoggingMXBeanSetLoggerLevelHandler() {
        parametersValidator.registerValidator(PlatformMBeanConstants.LOGGER_NAME, new ModelTypeValidator(ModelType.STRING, false, false));
        parametersValidator.registerValidator(PlatformMBeanConstants.LEVEL_NAME, new ModelTypeValidator(ModelType.STRING, true, false));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        /**
         * Implementation note: This implementation uses indirect access to the mbean (i.e. via the
         * {@code MBeanServerConnection} API) in order to avoid adding a compile time dependency on JDK 7. If the base
         * JDK requirement for JBoss AS ever moves to JDK 7, this implementation can be updated to use the
         * {@code java.lang.management.PlatformLoggingMXBean} interface.
         */

        parametersValidator.validate(operation);
        final String loggerName = operation.require(PlatformMBeanConstants.LOGGER_NAME).asString();
        final String levelName = operation.hasDefined(PlatformMBeanConstants.LEVEL_NAME)
                ? operation.require(PlatformMBeanConstants.LEVEL_NAME).asString() : null;
        try {
            ManagementFactory.getPlatformMBeanServer().invoke(PlatformMBeanConstants.PLATFORM_LOGGING_OBJECT_NAME,
                    "setLoggerLevel", new String[]{loggerName, levelName}, SIGNATURE);
            context.getResult();  // sets it as undefined for this void op
        } catch (JMRuntimeException e) {
            throw e;
        } catch (JMException e) {
            throw new RuntimeException(e);
        }

        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return PlatformMBeanDescriptions.getSetLoggerLevelDescription(locale);
    }
}
