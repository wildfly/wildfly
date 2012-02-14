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
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Executes the {@link java.lang.management.ThreadMXBean} {@code getThreadInfo} methods that return an array
 * of thread ids.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ThreadMXBeanThreadInfosHandler implements OperationStepHandler, DescriptionProvider {

    public static final ThreadMXBeanThreadInfosHandler INSTANCE = new ThreadMXBeanThreadInfosHandler();

    private final ParametersValidator idsValidator = new ParametersValidator();
    private final ParametersValidator depthValidator = new ParametersValidator();
    private final ParametersValidator lockedValidator = new ParametersValidator();
    private ThreadMXBeanThreadInfosHandler() {
        idsValidator.registerValidator(PlatformMBeanConstants.IDS, new ListValidator(new LongRangeValidator(1)));
        depthValidator.registerValidator(PlatformMBeanConstants.MAX_DEPTH, new IntRangeValidator(1, Integer.MAX_VALUE, false, false));
        lockedValidator.registerValidator(PlatformMBeanConstants.LOCKED_MONITORS, new ModelTypeValidator(ModelType.BOOLEAN));
        lockedValidator.registerValidator(PlatformMBeanConstants.LOCKED_SYNCHRONIZERS, new ModelTypeValidator(ModelType.BOOLEAN));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        try {
            final long[] ids = getIds(operation);
            ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] infos;
            if (operation.hasDefined(PlatformMBeanConstants.LOCKED_MONITORS)) {
                lockedValidator.validate(operation);
                infos = mbean.getThreadInfo(ids,
                            operation.require(PlatformMBeanConstants.LOCKED_MONITORS).asBoolean(),
                            operation.require(PlatformMBeanConstants.LOCKED_SYNCHRONIZERS).asBoolean());
            }else if (operation.hasDefined(PlatformMBeanConstants.MAX_DEPTH)) {
                depthValidator.validate(operation);
                infos = mbean.getThreadInfo(ids, operation.require(PlatformMBeanConstants.MAX_DEPTH).asInt());
            } else {
                infos = mbean.getThreadInfo(ids);
            }

            final ModelNode result = context.getResult();
            if (infos != null) {
                result.setEmptyList();
                for (ThreadInfo info : infos) {
                    if (info != null) {
                        result.add(PlatformMBeanUtil.getDetypedThreadInfo(info, mbean.isThreadCpuTimeSupported()));
                    } else {
                        // Add an undefined placeholder
                        result.add();
                    }
                }
            }
        } catch (SecurityException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }

        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return PlatformMBeanDescriptions.getGetThreadInfosDescription(locale);
    }

    private long[] getIds(final ModelNode operation) throws OperationFailedException {
        idsValidator.validate(operation);
        final List<ModelNode> idNodes = operation.require(PlatformMBeanConstants.IDS).asList();
        final long[] ids = new long[idNodes.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = idNodes.get(i).asLong();
        }
        return ids;
    }
}
