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
package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.threads.Constants.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.Constants.BLOCKING;
import static org.jboss.as.threads.Constants.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.Constants.CORE_THREADS_COUNT;
import static org.jboss.as.threads.Constants.CORE_THREADS_PER_CPU;
import static org.jboss.as.threads.Constants.GROUP_NAME;
import static org.jboss.as.threads.Constants.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.Constants.KEEPALIVE_TIME_DURATION;
import static org.jboss.as.threads.Constants.KEEPALIVE_TIME_UNIT;
import static org.jboss.as.threads.Constants.MAX_THREADS_COUNT;
import static org.jboss.as.threads.Constants.MAX_THREADS_PER_CPU;
import static org.jboss.as.threads.Constants.PRIORITY;
import static org.jboss.as.threads.Constants.PROPERTIES;
import static org.jboss.as.threads.Constants.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.Constants.QUEUE_LENGTH_COUNT;
import static org.jboss.as.threads.Constants.QUEUE_LENGTH_PER_CPU;
import static org.jboss.as.threads.Constants.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.Constants.THREAD_FACTORY;
import static org.jboss.as.threads.Constants.THREAD_NAME_PATTERN;
import static org.jboss.as.threads.Constants.UNBOUNDED_QUEUE_THREAD_POOL;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class NewThreadsSubsystemProviders {

    static final String RESOURCE_NAME = NewThreadsSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("threads"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.THREADS_1_0.getUriString());

            //Should this be an attribute instead
            subsystem.get(CHILDREN, THREAD_FACTORY, DESCRIPTION).set(bundle.getString("threadfactories"));
            subsystem.get(CHILDREN, THREAD_FACTORY, REQUIRED).set(false);

            subsystem.get(CHILDREN, UNBOUNDED_QUEUE_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.unbounded"));
            subsystem.get(CHILDREN, UNBOUNDED_QUEUE_THREAD_POOL, REQUIRED).set(false);

            subsystem.get(CHILDREN, BOUNDED_QUEUE_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.bounded"));
            subsystem.get(CHILDREN, BOUNDED_QUEUE_THREAD_POOL, REQUIRED).set(false);

            subsystem.get(CHILDREN, QUEUELESS_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.queueless"));
            subsystem.get(CHILDREN, QUEUELESS_THREAD_POOL, REQUIRED).set(false);

            subsystem.get(CHILDREN, SCHEDULED_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.scheduled"));
            subsystem.get(CHILDREN, SCHEDULED_THREAD_POOL, REQUIRED).set(false);

            return subsystem;
        }
    };

    static final DescriptionProvider THREAD_FACTORY_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("threadfactory"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("threadfactory.name"));
            node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, NAME, REQUIRED).set(true);

            node.get(ATTRIBUTES, GROUP_NAME, DESCRIPTION).set(bundle.getString("threadfactory.groupname"));
            node.get(ATTRIBUTES, GROUP_NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, GROUP_NAME, REQUIRED).set(false);

            node.get(ATTRIBUTES, THREAD_NAME_PATTERN, DESCRIPTION).set(bundle.getString("threadfactory.threadnamepattern"));
            node.get(ATTRIBUTES, THREAD_NAME_PATTERN, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, THREAD_NAME_PATTERN, REQUIRED).set(false);

            node.get(ATTRIBUTES, PRIORITY, DESCRIPTION).set(bundle.getString("threadfactory.priority"));
            node.get(ATTRIBUTES, PRIORITY, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, PRIORITY, REQUIRED).set(false);

            node.get(ATTRIBUTES, PROPERTIES, DESCRIPTION).set(bundle.getString("threadfactory.properties"));
            node.get(ATTRIBUTES, PROPERTIES, TYPE).set(ModelType.LIST);
            node.get(ATTRIBUTES, PROPERTIES, VALUE_TYPE).set(ModelType.PROPERTY);
            node.get(ATTRIBUTES, PROPERTIES, REQUIRED).set(false);

            return node;
        }
    };

    static DescriptionProvider BOUNDED_QUEUE_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode operation = getCommonThreadPool(bundle, bundle.getString("threadpool.bounded.description"));

            operation.get(ATTRIBUTES, BLOCKING, DESCRIPTION).set(bundle.getString("threadpool.bounded.blocking"));
            operation.get(ATTRIBUTES, BLOCKING, TYPE).set(ModelType.BOOLEAN);
            operation.get(ATTRIBUTES, BLOCKING, REQUIRED).set(false);

            operation.get(ATTRIBUTES, ALLOW_CORE_TIMEOUT, DESCRIPTION).set(bundle.getString("threadpool.bounded.allowcoretimeout"));
            operation.get(ATTRIBUTES, ALLOW_CORE_TIMEOUT, TYPE).set(ModelType.BOOLEAN);
            operation.get(ATTRIBUTES, ALLOW_CORE_TIMEOUT, REQUIRED).set(false);

            operation.get(ATTRIBUTES, HANDOFF_EXECUTOR, DESCRIPTION).set(bundle.getString("threadpool.bounded.handoffexecutor"));
            operation.get(ATTRIBUTES, HANDOFF_EXECUTOR, TYPE).set(ModelType.STRING);
            operation.get(ATTRIBUTES, HANDOFF_EXECUTOR, REQUIRED).set(false);

            operation.get(ATTRIBUTES, CORE_THREADS_COUNT, DESCRIPTION).set(bundle.getString("threadpool.bounded.corethreadscount"));
            operation.get(ATTRIBUTES, CORE_THREADS_COUNT, TYPE).set(ModelType.BIG_DECIMAL);
            operation.get(ATTRIBUTES, CORE_THREADS_COUNT, REQUIRED).set(false);
            operation.get(ATTRIBUTES, CORE_THREADS_PER_CPU, DESCRIPTION).set(bundle.getString("threadpool.bounded.corethreadspercpu"));
            operation.get(ATTRIBUTES, CORE_THREADS_PER_CPU, TYPE).set(ModelType.BIG_DECIMAL);
            operation.get(ATTRIBUTES, CORE_THREADS_PER_CPU, REQUIRED).set(false);

            operation.get(ATTRIBUTES, QUEUE_LENGTH_COUNT, DESCRIPTION).set(bundle.getString("threadpool.bounded.queuelengthcount"));
            operation.get(ATTRIBUTES, QUEUE_LENGTH_COUNT, TYPE).set(ModelType.BIG_DECIMAL);
            operation.get(ATTRIBUTES, QUEUE_LENGTH_COUNT, REQUIRED).set(true);
            operation.get(ATTRIBUTES, QUEUE_LENGTH_PER_CPU, DESCRIPTION).set(bundle.getString("threadpool.bounded.queuelengthpercpu"));
            operation.get(ATTRIBUTES, QUEUE_LENGTH_PER_CPU, TYPE).set(ModelType.BIG_DECIMAL);
            operation.get(ATTRIBUTES, QUEUE_LENGTH_PER_CPU, REQUIRED).set(true);

            return operation;
        }
    };

    static final DescriptionProvider UNBOUNDED_QUEUE_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            return getCommonThreadPool(bundle, bundle.getString("threadpool.unbounded.description"));
        }
    };

    static final DescriptionProvider QUEUELESS_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode node = getCommonThreadPool(bundle, bundle.getString("threadpool.queueless.description"));

            node.get(ATTRIBUTES, BLOCKING, DESCRIPTION).set(bundle.getString("threadpool.queueless.blocking"));
            node.get(ATTRIBUTES, BLOCKING, TYPE).set(ModelType.BOOLEAN);
            node.get(ATTRIBUTES, BLOCKING, REQUIRED).set(true);

            node.get(ATTRIBUTES, HANDOFF_EXECUTOR, DESCRIPTION).set(bundle.getString("threadpool.queueless.handoffexecutor"));
            node.get(ATTRIBUTES, HANDOFF_EXECUTOR, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, HANDOFF_EXECUTOR, REQUIRED).set(true);

            return node;
        }
    };

    static final DescriptionProvider SCHEDULED_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            return getCommonThreadPool(bundle, bundle.getString("threadpool.scheduled.description"));
        }
    };

    private static ModelNode getCommonThreadPool(final ResourceBundle bundle, final String description) {
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(description);
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);

        node.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("threadpool.common.name"));
        node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, NAME, REQUIRED).set(true);

        node.get(ATTRIBUTES, THREAD_FACTORY, DESCRIPTION).set(bundle.getString("threadpool.common.threadfactory"));
        node.get(ATTRIBUTES, THREAD_FACTORY, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, THREAD_FACTORY, REQUIRED).set(false);

        node.get(ATTRIBUTES, PROPERTIES, DESCRIPTION).set(bundle.getString("threadpool.common.properties"));
        node.get(ATTRIBUTES, PROPERTIES, TYPE).set(ModelType.LIST);
        node.get(ATTRIBUTES, PROPERTIES, VALUE_TYPE).set(ModelType.PROPERTY);
        node.get(ATTRIBUTES, PROPERTIES, REQUIRED).set(false);

        //These could perhaps be children?

        node.get(ATTRIBUTES, MAX_THREADS_COUNT, DESCRIPTION).set(bundle.getString("threadpool.common.maxthreadscount"));
        node.get(ATTRIBUTES, MAX_THREADS_COUNT, TYPE).set(ModelType.BIG_DECIMAL);
        node.get(ATTRIBUTES, MAX_THREADS_COUNT, REQUIRED).set(false);

        node.get(ATTRIBUTES, MAX_THREADS_PER_CPU, DESCRIPTION).set(bundle.getString("threadpool.common.maxthreadspercpu"));
        node.get(ATTRIBUTES, MAX_THREADS_PER_CPU, TYPE).set(ModelType.BIG_DECIMAL);
        node.get(ATTRIBUTES, MAX_THREADS_PER_CPU, REQUIRED).set(false);

        node.get(ATTRIBUTES, KEEPALIVE_TIME_DURATION, DESCRIPTION).set(bundle.getString("threadpool.common.keepaliveduration"));
        node.get(ATTRIBUTES, KEEPALIVE_TIME_DURATION, TYPE).set(ModelType.LONG);
        node.get(ATTRIBUTES, KEEPALIVE_TIME_DURATION, REQUIRED).set(false);

        node.get(ATTRIBUTES, KEEPALIVE_TIME_UNIT, DESCRIPTION).set(bundle.getString("threadpool.common.keepaliveunit"));
        node.get(ATTRIBUTES, KEEPALIVE_TIME_UNIT, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, KEEPALIVE_TIME_UNIT, REQUIRED).set(false);
        return node;
    }

    //Operations
    static final DescriptionProvider SUBSYSTEM_ADD_DESC = new DescriptionProvider() {

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

    static DescriptionProvider ADD_THREAD_FACTORY_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("threadfactory.add"));
            operation.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("threadfactory.name"));
            operation.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
            operation.get(REQUEST_PROPERTIES, GROUP_NAME, DESCRIPTION).set(bundle.getString("threadfactory.groupname"));
            operation.get(REQUEST_PROPERTIES, GROUP_NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, GROUP_NAME, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, THREAD_NAME_PATTERN, DESCRIPTION).set(bundle.getString("threadfactory.threadnamepattern"));
            operation.get(REQUEST_PROPERTIES, THREAD_NAME_PATTERN, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, THREAD_NAME_PATTERN, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, PRIORITY, DESCRIPTION).set(bundle.getString("threadfactory.priority"));
            operation.get(REQUEST_PROPERTIES, PRIORITY, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, PRIORITY, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, PROPERTIES, DESCRIPTION).set(bundle.getString("threadfactory.properties"));
            operation.get(REQUEST_PROPERTIES, PROPERTIES, TYPE).set(ModelType.LIST);
            operation.get(REQUEST_PROPERTIES, PROPERTIES, VALUE_TYPE).set(ModelType.PROPERTY);
            operation.get(REQUEST_PROPERTIES, PROPERTIES, REQUIRED).set(false);
            operation.get(REPLY_PROPERTIES).setEmptyObject();
            return operation;
        }
    };

    static DescriptionProvider REMOVE_THREAD_FACTORY_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("threadfactory.remove"));
            operation.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("threadfactory.remove"));
            operation.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
            operation.get(REPLY_PROPERTIES).setEmptyObject();
            return operation;
        }
    };

    static DescriptionProvider ADD_BOUNDED_QUEUE_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode operation = getCommonAddThreadPool(bundle, ADD, bundle.getString("threadpool.bounded.add"));

            operation.get(REQUEST_PROPERTIES, BLOCKING, DESCRIPTION).set(bundle.getString("threadpool.bounded.blocking"));
            operation.get(REQUEST_PROPERTIES, BLOCKING, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, BLOCKING, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, ALLOW_CORE_TIMEOUT, DESCRIPTION).set(bundle.getString("threadpool.bounded.description"));
            operation.get(REQUEST_PROPERTIES, ALLOW_CORE_TIMEOUT, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, ALLOW_CORE_TIMEOUT, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, DESCRIPTION).set(bundle.getString("threadpool.bounded.handoffexecutor"));
            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, CORE_THREADS_COUNT, DESCRIPTION).set(bundle.getString("threadpool.bounded.corethreadscount"));
            operation.get(REQUEST_PROPERTIES, CORE_THREADS_COUNT, TYPE).set(ModelType.BIG_DECIMAL);
            operation.get(REQUEST_PROPERTIES, CORE_THREADS_COUNT, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, CORE_THREADS_PER_CPU, DESCRIPTION).set(bundle.getString("threadpool.bounded.corethreadspercpu"));
            operation.get(REQUEST_PROPERTIES, CORE_THREADS_PER_CPU, TYPE).set(ModelType.BIG_DECIMAL);
            operation.get(REQUEST_PROPERTIES, CORE_THREADS_PER_CPU, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH_COUNT, DESCRIPTION).set(bundle.getString("threadpool.bounded.queuelengthcount"));
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH_COUNT, TYPE).set(ModelType.BIG_DECIMAL);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH_COUNT, REQUIRED).set(true);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH_PER_CPU, DESCRIPTION).set(bundle.getString("threadpool.bounded.queuelengthpercpu"));
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH_PER_CPU, TYPE).set(ModelType.BIG_DECIMAL);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH_PER_CPU, REQUIRED).set(true);

            return operation;
        }
    };


    static DescriptionProvider ADD_QUEUELESS_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode operation = getCommonAddThreadPool(bundle, ADD, bundle.getString("threadpool.queueless.add"));

            operation.get(REQUEST_PROPERTIES, BLOCKING, DESCRIPTION).set(bundle.getString("threadpool.queueless.blocking"));
            operation.get(REQUEST_PROPERTIES, BLOCKING, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, BLOCKING, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, DESCRIPTION).set(bundle.getString("threadpool.queueless.handoffexecutor"));
            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, REQUIRED).set(true);

            return operation;
        }
    };

    static DescriptionProvider ADD_UNBOUNDED_QUEUE_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            return getCommonAddThreadPool(bundle, ADD, bundle.getString("threadpool.unbounded.add"));
        }
    };

    static DescriptionProvider ADD_SCHEDULED_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            return getCommonAddThreadPool(bundle, ADD, bundle.getString("threadpool.scheduled.add"));
        }
    };

    static ModelNode getCommonAddThreadPool(final ResourceBundle bundle, final String operationName, final String description) {
        final ModelNode operation = new ModelNode();
        operation.get(OPERATION_NAME).set(operationName);
        operation.get(DESCRIPTION).set(description);
        operation.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("threadpool.common.name"));
        operation.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
        operation.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
        operation.get(REQUEST_PROPERTIES, THREAD_FACTORY, DESCRIPTION).set(bundle.getString("threadpool.common.threadfactory"));
        operation.get(REQUEST_PROPERTIES, THREAD_FACTORY, TYPE).set(ModelType.STRING);
        operation.get(REQUEST_PROPERTIES, THREAD_FACTORY, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, PROPERTIES, DESCRIPTION).set(bundle.getString("threadpool.common.properties"));
        operation.get(REQUEST_PROPERTIES, PROPERTIES, TYPE).set(ModelType.LIST);
        operation.get(REQUEST_PROPERTIES, PROPERTIES, VALUE_TYPE).set(ModelType.PROPERTY);
        operation.get(REQUEST_PROPERTIES, PROPERTIES, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, MAX_THREADS_COUNT, DESCRIPTION).set(bundle.getString("threadpool.common.maxthreadscount"));
        operation.get(REQUEST_PROPERTIES, MAX_THREADS_COUNT, TYPE).set(ModelType.BIG_DECIMAL);
        operation.get(REQUEST_PROPERTIES, MAX_THREADS_COUNT, REQUIRED).set(true);
        operation.get(REQUEST_PROPERTIES, MAX_THREADS_PER_CPU, DESCRIPTION).set(bundle.getString("threadpool.common.maxthreadspercpu"));
        operation.get(REQUEST_PROPERTIES, MAX_THREADS_PER_CPU, TYPE).set(ModelType.BIG_DECIMAL);
        operation.get(REQUEST_PROPERTIES, MAX_THREADS_PER_CPU, REQUIRED).set(true);
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME_DURATION, DESCRIPTION).set(bundle.getString("threadpool.common.keepaliveduration"));
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME_DURATION, TYPE).set(ModelType.LONG);
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME_DURATION, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME_UNIT, DESCRIPTION).set(bundle.getString("threadpool.common.keepaliveunit"));
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME_UNIT, TYPE).set(ModelType.STRING);
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME_UNIT, REQUIRED).set(false);
        operation.get(REPLY_PROPERTIES).setEmptyObject();
        return operation;
    }

    static DescriptionProvider REMOVE_BOUNDED_QUEUE_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            return getCommonRemoveThreadPool(bundle, REMOVE, bundle.getString("threadpool.bounded.remove"));
        }
    };

    static DescriptionProvider REMOVE_QUEUELESS_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            return getCommonRemoveThreadPool(bundle, REMOVE, bundle.getString("threadpool.queueless.remove"));
        }
    };

    static DescriptionProvider REMOVE_UNBOUNDED_QUEUE_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            return getCommonRemoveThreadPool(bundle, REMOVE, bundle.getString("threadpool.unbounded.remove"));
        }
    };

    static DescriptionProvider REMOVE_SCHEDULED_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            return getCommonRemoveThreadPool(bundle, REMOVE, bundle.getString("threadpool.scheduled.remove"));
        }
    };

    private static ModelNode getCommonRemoveThreadPool(final ResourceBundle bundle, String operationName, String description) {
        ModelNode operation = new ModelNode();
        operation.get(OPERATION_NAME).set(operationName);
        operation.get(DESCRIPTION).set(description);
        operation.get(REPLY_PROPERTIES).setEmptyObject();
        return operation;
    }


    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
