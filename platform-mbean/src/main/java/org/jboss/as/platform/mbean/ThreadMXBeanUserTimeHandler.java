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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;

/**
 * Executes the {@link java.lang.management.ThreadMXBean#getThreadUserTime(long)} method.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ThreadMXBeanUserTimeHandler implements OperationStepHandler, DescriptionProvider {

    public static final ThreadMXBeanUserTimeHandler INSTANCE = new ThreadMXBeanUserTimeHandler();

    private final ParametersValidator validator = new ParametersValidator();
    private ThreadMXBeanUserTimeHandler() {
        validator.registerValidator(PlatformMBeanConstants.ID, new LongRangeValidator(1));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);

        try {
            long id = operation.require(PlatformMBeanConstants.ID).asLong();
            context.getResult().set(ManagementFactory.getThreadMXBean().getThreadUserTime(id));
        } catch (UnsupportedOperationException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }

        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return PlatformMBeanDescriptions.getThreadUserTimeOperation(locale);
    }
}
