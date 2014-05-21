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
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Executes the {@link java.lang.management.ThreadMXBean} {@code getThreadInfo} methods that return a single thread id.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ThreadMXBeanThreadInfoHandler implements OperationStepHandler{
    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(PlatformMBeanConstants.GET_THREAD_INFO, PlatformMBeanUtil.getResolver(PlatformMBeanConstants.THREADING))
                  .setParameters(CommonAttributes.ID, CommonAttributes.MAX_DEPTH)
                  .setReplyType(ModelType.LIST)
                  .setReplyParameters(CommonAttributes.THREAD_INFO_ATTRIBUTES)
            .setReadOnly()
            .setRuntimeOnly()
                  .build();


    public static final ThreadMXBeanThreadInfoHandler INSTANCE = new ThreadMXBeanThreadInfoHandler();

    private final ParametersValidator validator = new ParametersValidator();
    private ThreadMXBeanThreadInfoHandler() {
        validator.registerValidator(PlatformMBeanConstants.ID, new LongRangeValidator(1));
        validator.registerValidator(PlatformMBeanConstants.MAX_DEPTH, new IntRangeValidator(1, Integer.MAX_VALUE, true, false));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        try {
            long id = operation.require(PlatformMBeanConstants.ID).asLong();
            ThreadInfo info;
            if (operation.hasDefined(PlatformMBeanConstants.MAX_DEPTH)) {
                info = mbean.getThreadInfo(id, operation.require(PlatformMBeanConstants.MAX_DEPTH).asInt());
            } else {
                info = mbean.getThreadInfo(id);
            }

            final ModelNode result = context.getResult();
            if (info != null) {
                result.set(PlatformMBeanUtil.getDetypedThreadInfo(info, mbean.isThreadCpuTimeSupported()));
            }
        } catch (SecurityException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }

        context.stepCompleted();
    }

}
