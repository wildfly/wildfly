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

package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class ThreadsAdd implements ModelAddOperationHandler, DescriptionProvider {

    static final ThreadsAdd INSTANCE = new ThreadsAdd();

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

        context.getSubModel().get(BOUNDED_QUEUE_THREAD_POOL).setEmptyObject();
        context.getSubModel().get(QUEUELESS_THREAD_POOL).setEmptyObject();
        context.getSubModel().get(SCHEDULED_THREAD_POOL).setEmptyObject();
        context.getSubModel().get(THREAD_FACTORY).setEmptyObject();
        context.getSubModel().get(UNBOUNDED_QUEUE_THREAD_POOL).setEmptyObject();

        // Compensating is remove
        final ModelNode compensating = Util.getResourceRemoveOperation(operation.require(ADDRESS));
        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensating);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ThreadsProvider.THREADS_ADD_DESC.getModelDescription(locale);
    }

    static class ThreadsProvider {
        static final String RESOURCE_NAME = ThreadsProvider.class.getPackage().getName() + ".LocalDescriptions";

        static final DescriptionProvider THREADS_ADD_DESC = new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(final Locale locale) {
                final ResourceBundle bundle = getResourceBundle(locale);
                final ModelNode operation = new ModelNode();
                operation.get(OPERATION_NAME).set("add");
                operation.get(DESCRIPTION).set(bundle.getString("threads.add"));
                operation.get(REQUEST_PROPERTIES).setEmptyObject();
                operation.get(REPLY_PROPERTIES).setEmptyObject();
                return operation;
            }
        };

        private static ResourceBundle getResourceBundle(Locale locale) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            return ResourceBundle.getBundle(RESOURCE_NAME, locale);
        }
    }
}
