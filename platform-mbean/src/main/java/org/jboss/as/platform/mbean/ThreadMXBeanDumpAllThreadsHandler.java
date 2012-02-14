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
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Executes the {@link java.lang.management.ThreadMXBean#dumpAllThreads(boolean, boolean)} method.
 * of thread ids.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ThreadMXBeanDumpAllThreadsHandler implements OperationStepHandler, DescriptionProvider {

    public static final ThreadMXBeanDumpAllThreadsHandler INSTANCE = new ThreadMXBeanDumpAllThreadsHandler();

    private final ParametersValidator lockedValidator = new ParametersValidator();

    private ThreadMXBeanDumpAllThreadsHandler() {
        lockedValidator.registerValidator(PlatformMBeanConstants.LOCKED_MONITORS, new ModelTypeValidator(ModelType.BOOLEAN));
        lockedValidator.registerValidator(PlatformMBeanConstants.LOCKED_SYNCHRONIZERS, new ModelTypeValidator(ModelType.BOOLEAN));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        lockedValidator.validate(operation);

        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        try {
            ThreadInfo[] infos = mbean.dumpAllThreads(
                                    operation.require(PlatformMBeanConstants.LOCKED_MONITORS).asBoolean(),
                                    operation.require(PlatformMBeanConstants.LOCKED_SYNCHRONIZERS).asBoolean());

            final ModelNode result = context.getResult();
            if (infos != null) {
                result.setEmptyList();
                for (ThreadInfo info : infos) {
                    result.add(PlatformMBeanUtil.getDetypedThreadInfo(info, mbean.isThreadCpuTimeSupported()));
                }
            }
        } catch (SecurityException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        } catch (UnsupportedOperationException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }

        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return PlatformMBeanDescriptions.getDumpThreadsDescription(locale);
    }
}
