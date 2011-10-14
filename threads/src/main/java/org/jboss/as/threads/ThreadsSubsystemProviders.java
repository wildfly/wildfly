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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
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
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.COUNT;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.PER_CPU;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.UNIT;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ThreadsSubsystemProviders {

    static final String RESOURCE_NAME = ThreadsSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM_PROVIDER = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("threads"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.THREADS_1_0.getUriString());

            subsystem.get(CHILDREN, THREAD_FACTORY, DESCRIPTION).set(bundle.getString("threadfactories"));
            subsystem.get(CHILDREN, THREAD_FACTORY, MIN_OCCURS).set(0);
            subsystem.get(CHILDREN, THREAD_FACTORY, MAX_OCCURS).set(Integer.MAX_VALUE);

            subsystem.get(CHILDREN, UNBOUNDED_QUEUE_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.unbounded"));
            subsystem.get(CHILDREN, UNBOUNDED_QUEUE_THREAD_POOL, MIN_OCCURS).set(0);
            subsystem.get(CHILDREN, UNBOUNDED_QUEUE_THREAD_POOL, MAX_OCCURS).set(Integer.MAX_VALUE);

            subsystem.get(CHILDREN, BOUNDED_QUEUE_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.bounded"));
            subsystem.get(CHILDREN, BOUNDED_QUEUE_THREAD_POOL, MIN_OCCURS).set(0);
            subsystem.get(CHILDREN, BOUNDED_QUEUE_THREAD_POOL, MAX_OCCURS).set(Integer.MAX_VALUE);

            subsystem.get(CHILDREN, QUEUELESS_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.queueless"));
            subsystem.get(CHILDREN, QUEUELESS_THREAD_POOL, MIN_OCCURS).set(0);
            subsystem.get(CHILDREN, QUEUELESS_THREAD_POOL, MAX_OCCURS).set(Integer.MAX_VALUE);

            subsystem.get(CHILDREN, SCHEDULED_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.scheduled"));
            subsystem.get(CHILDREN, SCHEDULED_THREAD_POOL, MIN_OCCURS).set(0);
            subsystem.get(CHILDREN, SCHEDULED_THREAD_POOL, MAX_OCCURS).set(Integer.MAX_VALUE);

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

            for(AttributeDefinition attr : PoolAttributeDefinitions.THREAD_FACTORY_ATTRIBUTES) {
                attr.addResourceAttributeDescription(bundle, "threadfactory", node);
            }
/*            node.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("threadfactory.name"));
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
            node.get(ATTRIBUTES, PRIORITY, DEFAULT).set(-1);

            node.get(ATTRIBUTES, PROPERTIES, DESCRIPTION).set(bundle.getString("threadfactory.properties"));
            node.get(ATTRIBUTES, PROPERTIES, TYPE).set(ModelType.OBJECT);
            node.get(ATTRIBUTES, PROPERTIES, VALUE_TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, PROPERTIES, REQUIRED).set(false);
*/
            return node;
        }
    };

    public static DescriptionProvider BOUNDED_QUEUE_THREAD_POOL_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode operation = getCommonThreadPool(bundle, bundle.getString("threadpool.bounded.description"));

            operation.get(ATTRIBUTES, BLOCKING, DESCRIPTION).set(bundle.getString("threadpool.bounded.blocking"));
            operation.get(ATTRIBUTES, BLOCKING, TYPE).set(ModelType.BOOLEAN);
            operation.get(ATTRIBUTES, BLOCKING, REQUIRED).set(false);

            operation.get(ATTRIBUTES, ALLOW_CORE_TIMEOUT, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.allowcoretimeout"));
            operation.get(ATTRIBUTES, ALLOW_CORE_TIMEOUT, TYPE).set(ModelType.BOOLEAN);
            operation.get(ATTRIBUTES, ALLOW_CORE_TIMEOUT, REQUIRED).set(false);

            operation.get(ATTRIBUTES, HANDOFF_EXECUTOR, DESCRIPTION)
                    .set(bundle.getString("threadpool.bounded.handoffexecutor"));
            operation.get(ATTRIBUTES, HANDOFF_EXECUTOR, TYPE).set(ModelType.STRING);
            operation.get(ATTRIBUTES, HANDOFF_EXECUTOR, REQUIRED).set(false);

            operation.get(ATTRIBUTES, CORE_THREADS, DESCRIPTION).set(bundle.getString("threadpool.bounded.corethreads"));
            operation.get(ATTRIBUTES, CORE_THREADS, TYPE).set(ModelType.OBJECT);
            operation.get(ATTRIBUTES, CORE_THREADS, REQUIRED).set(false);
            operation.get(ATTRIBUTES, CORE_THREADS, VALUE_TYPE, COUNT, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.corethreads.count"));
            operation.get(ATTRIBUTES, CORE_THREADS, VALUE_TYPE, COUNT, TYPE).set(ModelType.INT);
            operation.get(ATTRIBUTES, CORE_THREADS, VALUE_TYPE, COUNT, REQUIRED).set(true);
            operation.get(ATTRIBUTES, CORE_THREADS, VALUE_TYPE, PER_CPU, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.corethreads.percpu"));
            operation.get(ATTRIBUTES, CORE_THREADS, VALUE_TYPE, PER_CPU, TYPE).set(ModelType.INT);
            operation.get(ATTRIBUTES, CORE_THREADS, VALUE_TYPE, PER_CPU, REQUIRED).set(true);

            operation.get(ATTRIBUTES, QUEUE_LENGTH, DESCRIPTION).set(bundle.getString("threadpool.bounded.queuelength"));
            operation.get(ATTRIBUTES, QUEUE_LENGTH, TYPE).set(ModelType.OBJECT);
            operation.get(ATTRIBUTES, QUEUE_LENGTH, REQUIRED).set(true);
            operation.get(ATTRIBUTES, QUEUE_LENGTH, VALUE_TYPE, COUNT, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.queuelength.count"));
            operation.get(ATTRIBUTES, QUEUE_LENGTH, VALUE_TYPE, COUNT, TYPE).set(ModelType.INT);
            operation.get(ATTRIBUTES, QUEUE_LENGTH, VALUE_TYPE, COUNT, REQUIRED).set(true);
            operation.get(ATTRIBUTES, QUEUE_LENGTH, VALUE_TYPE, PER_CPU, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.queuelength.percpu"));
            operation.get(ATTRIBUTES, QUEUE_LENGTH, VALUE_TYPE, PER_CPU, TYPE).set(ModelType.INT);
            operation.get(ATTRIBUTES, QUEUE_LENGTH, VALUE_TYPE, PER_CPU, REQUIRED).set(true);

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
        node.get(ATTRIBUTES, PROPERTIES, TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, PROPERTIES, VALUE_TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, PROPERTIES, REQUIRED).set(false);

        node.get(ATTRIBUTES, MAX_THREADS, DESCRIPTION).set(bundle.getString("threadpool.common.maxthreads"));
        node.get(ATTRIBUTES, MAX_THREADS, TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, MAX_THREADS, REQUIRED).set(true);
        node.get(ATTRIBUTES, MAX_THREADS, VALUE_TYPE, COUNT, DESCRIPTION).set(
                bundle.getString("threadpool.common.maxthreads.count"));
        node.get(ATTRIBUTES, MAX_THREADS, VALUE_TYPE, COUNT, TYPE).set(ModelType.INT);
        node.get(ATTRIBUTES, MAX_THREADS, VALUE_TYPE, COUNT, REQUIRED).set(true);
        node.get(ATTRIBUTES, MAX_THREADS, VALUE_TYPE, PER_CPU, DESCRIPTION).set(
                bundle.getString("threadpool.common.maxthreads.percpu"));
        node.get(ATTRIBUTES, MAX_THREADS, VALUE_TYPE, PER_CPU, TYPE).set(ModelType.INT);
        node.get(ATTRIBUTES, MAX_THREADS, VALUE_TYPE, PER_CPU, REQUIRED).set(true);

        node.get(ATTRIBUTES, KEEPALIVE_TIME, DESCRIPTION).set(bundle.getString("threadpool.common.keepalive"));
        node.get(ATTRIBUTES, KEEPALIVE_TIME, TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, KEEPALIVE_TIME, REQUIRED).set(false);
        node.get(ATTRIBUTES, KEEPALIVE_TIME, VALUE_TYPE, TIME, DESCRIPTION).set(
                bundle.getString("threadpool.common.keepalive.time"));
        node.get(ATTRIBUTES, KEEPALIVE_TIME, VALUE_TYPE, TIME, TYPE).set(ModelType.LONG);
        node.get(ATTRIBUTES, KEEPALIVE_TIME, VALUE_TYPE, TIME, REQUIRED).set(true);
        node.get(ATTRIBUTES, KEEPALIVE_TIME, VALUE_TYPE, UNIT, DESCRIPTION).set(
                bundle.getString("threadpool.common.keepalive.unit"));
        node.get(ATTRIBUTES, KEEPALIVE_TIME, VALUE_TYPE, UNIT, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, KEEPALIVE_TIME, VALUE_TYPE, UNIT, REQUIRED).set(true);
        return node;
    }

    // Operations
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

            for(AttributeDefinition attr : ThreadFactoryAdd.ATTRIBUTES) {
                attr.addOperationParameterDescription(bundle, "threadfactory", operation);
            }
/*            operation.get(REQUEST_PROPERTIES, GROUP_NAME, DESCRIPTION).set(bundle.getString("threadfactory.groupname"));
            operation.get(REQUEST_PROPERTIES, GROUP_NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, GROUP_NAME, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, THREAD_NAME_PATTERN, DESCRIPTION).set(
                    bundle.getString("threadfactory.threadnamepattern"));
            operation.get(REQUEST_PROPERTIES, THREAD_NAME_PATTERN, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, THREAD_NAME_PATTERN, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, PRIORITY, DESCRIPTION).set(bundle.getString("threadfactory.priority"));
            operation.get(REQUEST_PROPERTIES, PRIORITY, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, PRIORITY, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, PROPERTIES, DESCRIPTION).set(bundle.getString("threadfactory.properties"));
            operation.get(REQUEST_PROPERTIES, PROPERTIES, TYPE).set(ModelType.OBJECT);
            operation.get(REQUEST_PROPERTIES, PROPERTIES, VALUE_TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, PROPERTIES, REQUIRED).set(false);
*/            operation.get(REPLY_PROPERTIES).setEmptyObject();
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

            operation.get(REQUEST_PROPERTIES, ALLOW_CORE_TIMEOUT, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.allowcoretimeout"));
            operation.get(REQUEST_PROPERTIES, ALLOW_CORE_TIMEOUT, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, ALLOW_CORE_TIMEOUT, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.handoffexecutor"));
            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, CORE_THREADS, DESCRIPTION)
                    .set(bundle.getString("threadpool.bounded.corethreads"));
            operation.get(REQUEST_PROPERTIES, CORE_THREADS, TYPE).set(ModelType.OBJECT);
            operation.get(REQUEST_PROPERTIES, CORE_THREADS, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, CORE_THREADS, VALUE_TYPE, COUNT, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.corethreads.count"));
            operation.get(REQUEST_PROPERTIES, CORE_THREADS, VALUE_TYPE, COUNT, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, CORE_THREADS, VALUE_TYPE, COUNT, REQUIRED).set(true);
            operation.get(REQUEST_PROPERTIES, CORE_THREADS, VALUE_TYPE, PER_CPU, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.corethreads.percpu"));
            operation.get(REQUEST_PROPERTIES, CORE_THREADS, VALUE_TYPE, PER_CPU, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, CORE_THREADS, VALUE_TYPE, PER_CPU, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, DESCRIPTION)
                    .set(bundle.getString("threadpool.bounded.queuelength"));
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, TYPE).set(ModelType.OBJECT);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, REQUIRED).set(true);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, VALUE_TYPE, COUNT, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.queuelength.count"));
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, VALUE_TYPE, COUNT, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, VALUE_TYPE, COUNT, REQUIRED).set(true);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, VALUE_TYPE, PER_CPU, DESCRIPTION).set(
                    bundle.getString("threadpool.bounded.queuelength.percpu"));
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, VALUE_TYPE, PER_CPU, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, VALUE_TYPE, PER_CPU, REQUIRED).set(true);

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

            operation.get(REQUEST_PROPERTIES, HANDOFF_EXECUTOR, DESCRIPTION).set(
                    bundle.getString("threadpool.queueless.handoffexecutor"));
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
        operation.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(false);

        operation.get(REQUEST_PROPERTIES, THREAD_FACTORY, DESCRIPTION).set(bundle.getString("threadpool.common.threadfactory"));
        operation.get(REQUEST_PROPERTIES, THREAD_FACTORY, TYPE).set(ModelType.STRING);
        operation.get(REQUEST_PROPERTIES, THREAD_FACTORY, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, PROPERTIES, DESCRIPTION).set(bundle.getString("threadpool.common.properties"));
        operation.get(REQUEST_PROPERTIES, PROPERTIES, TYPE).set(ModelType.OBJECT);
        operation.get(REQUEST_PROPERTIES, PROPERTIES, VALUE_TYPE).set(ModelType.STRING);
        operation.get(REQUEST_PROPERTIES, PROPERTIES, REQUIRED).set(false);

        operation.get(REQUEST_PROPERTIES, MAX_THREADS, DESCRIPTION).set(bundle.getString("threadpool.common.maxthreads"));
        operation.get(REQUEST_PROPERTIES, MAX_THREADS, TYPE).set(ModelType.OBJECT);
        operation.get(REQUEST_PROPERTIES, MAX_THREADS, REQUIRED).set(true);
        operation.get(REQUEST_PROPERTIES, MAX_THREADS, VALUE_TYPE, COUNT, DESCRIPTION).set(
                bundle.getString("threadpool.common.maxthreads.count"));
        operation.get(REQUEST_PROPERTIES, MAX_THREADS, VALUE_TYPE, COUNT, TYPE).set(ModelType.INT);
        operation.get(REQUEST_PROPERTIES, MAX_THREADS, VALUE_TYPE, COUNT, REQUIRED).set(true);
        operation.get(REQUEST_PROPERTIES, MAX_THREADS, VALUE_TYPE, PER_CPU, DESCRIPTION).set(
                bundle.getString("threadpool.common.maxthreads.percpu"));
        operation.get(REQUEST_PROPERTIES, MAX_THREADS, VALUE_TYPE, PER_CPU, TYPE).set(ModelType.INT);
        operation.get(REQUEST_PROPERTIES, MAX_THREADS, VALUE_TYPE, PER_CPU, REQUIRED).set(true);

        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME, DESCRIPTION).set(bundle.getString("threadpool.common.keepalive"));
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME, TYPE).set(ModelType.OBJECT);
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME, VALUE_TYPE, TIME, DESCRIPTION).set(
                bundle.getString("threadpool.common.keepalive.time"));
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME, VALUE_TYPE, TIME, TYPE).set(ModelType.LONG);
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME, VALUE_TYPE, TIME, REQUIRED).set(true);
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME, VALUE_TYPE, UNIT, DESCRIPTION).set(
                bundle.getString("threadpool.common.keepalive.unit"));
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME, VALUE_TYPE, UNIT, TYPE).set(ModelType.STRING);
        operation.get(REQUEST_PROPERTIES, KEEPALIVE_TIME, VALUE_TYPE, UNIT, REQUIRED).set(true);

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

    public static ModelNode addThreadsDescriptionsToNode(final Locale locale, final ModelNode node) {
        final ResourceBundle bundle = getResourceBundle(locale);

        // Should this be an attribute instead
        node.get(CHILDREN, THREAD_FACTORY, DESCRIPTION).set(bundle.getString("threadfactories"));
        node.get(CHILDREN, THREAD_FACTORY, REQUIRED).set(false);

        node.get(CHILDREN, UNBOUNDED_QUEUE_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.unbounded"));
        node.get(CHILDREN, UNBOUNDED_QUEUE_THREAD_POOL, REQUIRED).set(false);

        node.get(CHILDREN, BOUNDED_QUEUE_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.bounded"));
        node.get(CHILDREN, BOUNDED_QUEUE_THREAD_POOL, REQUIRED).set(false);

        node.get(CHILDREN, QUEUELESS_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.queueless"));
        node.get(CHILDREN, QUEUELESS_THREAD_POOL, REQUIRED).set(false);

        node.get(CHILDREN, SCHEDULED_THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool.scheduled"));
        node.get(CHILDREN, SCHEDULED_THREAD_POOL, REQUIRED).set(false);

        return node;
    }
}
