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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.BLOCKED_COUNT;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.BLOCKED_TIME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.CLASS_NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.FILE_NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.GET_LOGGER_LEVEL;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.GET_PARENT_LOGGER_NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.GET_THREAD_INFOS;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.IDENTITY_HASH_CODE;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.IN_NATIVE;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LEVEL_NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LINE_NUMBER;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOCKED_MONITORS;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOCKED_STACK_DEPTH;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOCKED_STACK_FRAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOCKED_SYNCHRONIZERS;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOCK_INFO;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOCK_NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOCK_OWNER_ID;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOCK_OWNER_NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOGGER_NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.LOGGING;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.METHOD_NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.NATIVE_METHOD;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.SET_LOGGER_LEVEL;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.STACK_TRACE;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.SUSPENDED;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.THREAD_ID;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.THREAD_NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.THREAD_STATE;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.WAITED_COUNT;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.WAITED_TIME;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Static methods for creating domain management API descriptions for platform mbean resources.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PlatformMBeanDescriptions {

    static final String RESOURCE_NAME = PlatformMBeanDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder("");
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, PlatformMBeanDescriptions.class.getClassLoader(), true, false);
    }

    private PlatformMBeanDescriptions() {
    }

    public static ModelNode getGetThreadInfoDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = getThreadInfoOperation(bundle, PlatformMBeanConstants.GET_THREAD_INFO, PlatformMBeanConstants.THREADING);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.MAX_DEPTH));
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, TYPE).set(ModelType.INT);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, MIN).set(1);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, DEFAULT).set(0);

        final ModelNode reply = node.get(REPLY_PROPERTIES);
        reply.get(DESCRIPTION).set(bundle.getString("threading.get-thread-info.reply"));
        reply.get(TYPE).set(ModelType.OBJECT);
        reply.get(NILLABLE).set(true);
        populateThreadInfo(reply.get(VALUE_TYPE), bundle);

        return node;
    }

    public static ModelNode getGetThreadInfosDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(GET_THREAD_INFOS);
        node.get(DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.GET_THREAD_INFOS));

        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.IDS, DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.IDS));
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.IDS, TYPE).set(ModelType.LIST);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.IDS, VALUE_TYPE).set(ModelType.LONG);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.IDS, REQUIRED).set(true);

        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.MAX_DEPTH));
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, TYPE).set(ModelType.INT);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, MIN).set(1);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.MAX_DEPTH, DEFAULT).set(0);

        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_MONITORS, DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.LOCKED_MONITORS));
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_MONITORS, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_MONITORS, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_MONITORS, DEFAULT).set(false);

        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_SYNCHRONIZERS, DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.LOCKED_MONITORS));
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_SYNCHRONIZERS, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_SYNCHRONIZERS, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_SYNCHRONIZERS, DEFAULT).set(false);

        final ModelNode reply = node.get(REPLY_PROPERTIES);
        reply.get(DESCRIPTION).set(bundle.getString("threading.get-thread-infos.reply"));
        reply.get(TYPE).set(ModelType.LIST);
        reply.get(NILLABLE).set(false);
        populateThreadInfo(reply.get(VALUE_TYPE), bundle);

        return node;
    }

    public static ModelNode getDumpThreadsDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(PlatformMBeanConstants.DUMP_ALL_THREADS);
        node.get(DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.DUMP_ALL_THREADS));

        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_MONITORS, DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.LOCKED_MONITORS));
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_MONITORS, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_MONITORS, REQUIRED).set(true);

        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_SYNCHRONIZERS, DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.LOCKED_SYNCHRONIZERS));
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_SYNCHRONIZERS, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.LOCKED_SYNCHRONIZERS, REQUIRED).set(true);

        final ModelNode reply = node.get(REPLY_PROPERTIES);
        reply.get(DESCRIPTION).set(bundle.getString("threading.dump-threads.reply"));
        reply.get(TYPE).set(ModelType.LIST);
        reply.get(NILLABLE).set(false);
        populateThreadInfo(reply.get(VALUE_TYPE), bundle);

        return node;
    }

    public static ModelNode getThreadCpuTimeOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = getThreadInfoOperation(bundle, PlatformMBeanConstants.GET_THREAD_CPU_TIME, PlatformMBeanConstants.THREADING);

        node.get(REPLY_PROPERTIES, TYPE).set(ModelType.LONG);

        return node;
    }

    public static ModelNode getThreadUserTimeOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = getThreadInfoOperation(bundle, PlatformMBeanConstants.GET_THREAD_USER_TIME, PlatformMBeanConstants.THREADING);

        node.get(REPLY_PROPERTIES, TYPE).set(ModelType.LONG);

        return node;
    }

    public static ModelNode getFindThreadsOperation(final Locale locale, final String name, final String descriptionKeyBase) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(name);
        node.get(DESCRIPTION).set(bundle.getString(descriptionKeyBase + "." + name));

        node.get(REQUEST_PROPERTIES).setEmptyObject();

        final ModelNode reply = node.get(REPLY_PROPERTIES);
        reply.get(DESCRIPTION).set(bundle.getString("threading.find-threads.reply"));
        reply.get(TYPE).set(ModelType.LIST);
        reply.get(VALUE_TYPE).set(ModelType.LONG);
        reply.get(NILLABLE).set(true);

        return node;
    }

    private static ModelNode getThreadInfoOperation(final ResourceBundle bundle, final String name, final String descriptionKeyBase) {

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(name);
        node.get(DESCRIPTION).set(bundle.getString(descriptionKeyBase + "." + name));

        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.ID, DESCRIPTION).set(bundle.getString(PlatformMBeanConstants.THREADING + "." + PlatformMBeanConstants.ID));
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.ID, TYPE).set(ModelType.LONG);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.ID, MIN).set(1);
        node.get(REQUEST_PROPERTIES, PlatformMBeanConstants.ID, REQUIRED).set(true);

        return node;
    }

    private static void populateThreadInfo(final ModelNode toPopulate, ResourceBundle bundle) {
        toPopulate.get(THREAD_ID, DESCRIPTION).set(bundle.getString("threading.thread-info.thread-id"));
        toPopulate.get(THREAD_ID, TYPE).set(ModelType.LONG);
        toPopulate.get(THREAD_ID, NILLABLE).set(true);
        toPopulate.get(THREAD_NAME, DESCRIPTION).set(bundle.getString("threading.thread-info.thread-name"));
        toPopulate.get(THREAD_NAME, TYPE).set(ModelType.STRING);
        toPopulate.get(THREAD_NAME, NILLABLE).set(true);
        final ModelNode threadState = toPopulate.get(THREAD_STATE);
        threadState.get(DESCRIPTION).set(bundle.getString("threading.thread-info.thread-state"));
        threadState.get(TYPE).set(ModelType.STRING);
        threadState.get(NILLABLE).set(true);
        final ModelNode allowed = threadState.get(ALLOWED);
        for (Thread.State state : Thread.State.values()) {
            allowed.add(state.name());
        }
        toPopulate.get(BLOCKED_TIME, DESCRIPTION).set(bundle.getString("threading.thread-info.blocked-time"));
        toPopulate.get(BLOCKED_TIME, TYPE).set(ModelType.LONG);
        toPopulate.get(BLOCKED_TIME, NILLABLE).set(false);
        toPopulate.get(BLOCKED_TIME, UNIT).set(MeasurementUnit.MILLISECONDS.getName());
        toPopulate.get(BLOCKED_COUNT, DESCRIPTION).set(bundle.getString("threading.thread-info.blocked-count"));
        toPopulate.get(BLOCKED_COUNT, TYPE).set(ModelType.LONG);
        toPopulate.get(BLOCKED_COUNT, NILLABLE).set(true);
        toPopulate.get(WAITED_TIME, DESCRIPTION).set(bundle.getString("threading.thread-info.waited-time"));
        toPopulate.get(WAITED_TIME, TYPE).set(ModelType.LONG);
        toPopulate.get(WAITED_TIME, NILLABLE).set(false);
        toPopulate.get(WAITED_TIME, UNIT).set(MeasurementUnit.MILLISECONDS.getName());
        toPopulate.get(WAITED_COUNT, DESCRIPTION).set(bundle.getString("threading.thread-info.waited-count"));
        toPopulate.get(WAITED_COUNT, TYPE).set(ModelType.STRING);
        toPopulate.get(WAITED_COUNT, NILLABLE).set(true);
        toPopulate.get(LOCK_INFO, DESCRIPTION).set(bundle.getString("threading.thread-info.lock-info"));
        toPopulate.get(LOCK_INFO, TYPE).set(ModelType.OBJECT);
        toPopulate.get(LOCK_INFO, NILLABLE).set(true);
        populateLockInfo(toPopulate.get(LOCK_INFO, VALUE_TYPE), bundle);
        toPopulate.get(LOCK_NAME, DESCRIPTION).set(bundle.getString("threading.thread-info.lock-name"));
        toPopulate.get(LOCK_NAME, TYPE).set(ModelType.STRING);
        toPopulate.get(LOCK_NAME, NILLABLE).set(true);
        toPopulate.get(LOCK_OWNER_ID, DESCRIPTION).set(bundle.getString("threading.thread-info.lock-owner-id"));
        toPopulate.get(LOCK_OWNER_ID, TYPE).set(ModelType.LONG);
        toPopulate.get(LOCK_OWNER_ID, NILLABLE).set(true);
        toPopulate.get(LOCK_OWNER_NAME, DESCRIPTION).set(bundle.getString("threading.thread-info.lock-owner-name"));
        toPopulate.get(LOCK_OWNER_NAME, TYPE).set(ModelType.STRING);
        toPopulate.get(LOCK_OWNER_NAME, NILLABLE).set(true);
        toPopulate.get(STACK_TRACE, DESCRIPTION).set(bundle.getString("threading.thread-info.stack-trace"));
        toPopulate.get(STACK_TRACE, TYPE).set(ModelType.LIST);
        toPopulate.get(STACK_TRACE, NILLABLE).set(true);
        populateStackTraceElement(toPopulate.get(STACK_TRACE, VALUE_TYPE), bundle);
        toPopulate.get(SUSPENDED, DESCRIPTION).set(bundle.getString("threading.thread-info.suspended"));
        toPopulate.get(SUSPENDED, TYPE).set(ModelType.BOOLEAN);
        toPopulate.get(SUSPENDED, NILLABLE).set(true);
        toPopulate.get(IN_NATIVE, DESCRIPTION).set(bundle.getString("threading.thread-info.in-native"));
        toPopulate.get(IN_NATIVE, TYPE).set(ModelType.BOOLEAN);
        toPopulate.get(IN_NATIVE, NILLABLE).set(true);
        toPopulate.get(LOCKED_MONITORS, DESCRIPTION).set(bundle.getString("threading.thread-info.locked-monitors"));
        toPopulate.get(LOCKED_MONITORS, TYPE).set(ModelType.LIST);
        toPopulate.get(LOCKED_MONITORS, NILLABLE).set(true);
        populateMonitorInfo(toPopulate.get(LOCKED_MONITORS, VALUE_TYPE), bundle);
        toPopulate.get(LOCKED_SYNCHRONIZERS, DESCRIPTION).set(bundle.getString("threading.thread-info.locked-synchronizers"));
        toPopulate.get(LOCKED_SYNCHRONIZERS, TYPE).set(ModelType.LIST);
        toPopulate.get(LOCKED_SYNCHRONIZERS, NILLABLE).set(true);
        populateLockInfo(toPopulate.get(LOCKED_SYNCHRONIZERS, VALUE_TYPE), bundle);
    }

    private static void populateLockInfo(final ModelNode toPopulate, ResourceBundle bundle) {
        toPopulate.get(CLASS_NAME, DESCRIPTION).set(bundle.getString("threading.lock.class-name"));
        toPopulate.get(CLASS_NAME, TYPE).set(ModelType.STRING);
        toPopulate.get(CLASS_NAME, NILLABLE).set(true);
        toPopulate.get(IDENTITY_HASH_CODE, DESCRIPTION).set(bundle.getString("threading.lock.identity-hash-code"));
        toPopulate.get(IDENTITY_HASH_CODE, TYPE).set(ModelType.INT);
        toPopulate.get(IDENTITY_HASH_CODE, NILLABLE).set(true);
    }

    private static void populateMonitorInfo(final ModelNode toPopulate, ResourceBundle bundle) {
        populateLockInfo(toPopulate, bundle);
        toPopulate.get(LOCKED_STACK_DEPTH, DESCRIPTION).set(bundle.getString("threading.monitor.locked-stack-depth"));
        toPopulate.get(LOCKED_STACK_DEPTH, TYPE).set(ModelType.INT);
        toPopulate.get(LOCKED_STACK_DEPTH, NILLABLE).set(true);
        toPopulate.get(LOCKED_STACK_FRAME, DESCRIPTION).set(bundle.getString("threading.monitor.locked-stack-frame"));
        toPopulate.get(LOCKED_STACK_FRAME, TYPE).set(ModelType.OBJECT);
        toPopulate.get(LOCKED_STACK_FRAME, NILLABLE).set(true);
        final ModelNode valType = toPopulate.get(LOCKED_STACK_FRAME, VALUE_TYPE);
        populateStackTraceElement(valType, bundle);
    }

    private static void populateStackTraceElement(final ModelNode toPopulate, ResourceBundle bundle) {
        toPopulate.get(FILE_NAME, DESCRIPTION).set(bundle.getString("threading.stack.file-name"));
        toPopulate.get(FILE_NAME, TYPE).set(ModelType.STRING);
        toPopulate.get(FILE_NAME, NILLABLE).set(true);
        toPopulate.get(LINE_NUMBER, DESCRIPTION).set(bundle.getString("threading.stack.line-number"));
        toPopulate.get(LINE_NUMBER, TYPE).set(ModelType.INT);
        toPopulate.get(LINE_NUMBER, NILLABLE).set(true);
        toPopulate.get(CLASS_NAME, DESCRIPTION).set(bundle.getString("threading.stack.class-name"));
        toPopulate.get(CLASS_NAME, TYPE).set(ModelType.STRING);
        toPopulate.get(CLASS_NAME, NILLABLE).set(true);
        toPopulate.get(METHOD_NAME, DESCRIPTION).set(bundle.getString("threading.stack.method-name"));
        toPopulate.get(METHOD_NAME, TYPE).set(ModelType.STRING);
        toPopulate.get(METHOD_NAME, NILLABLE).set(true);
        toPopulate.get(NATIVE_METHOD, DESCRIPTION).set(bundle.getString("threading.stack.native-method"));
        toPopulate.get(NATIVE_METHOD, TYPE).set(ModelType.STRING);
        toPopulate.get(NATIVE_METHOD, NILLABLE).set(true);
    }

    public static ModelNode getGetLoggerLevelDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(GET_LOGGER_LEVEL);
        node.get(DESCRIPTION).set(bundle.getString("logging.get-logger-level"));

        addLoggerNameParam(node.get(REQUEST_PROPERTIES), bundle);

        node.get(REPLY_PROPERTIES);
        node.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        node.get(REPLY_PROPERTIES, NILLABLE).set(true);

        return node;
    }

    public static ModelNode getSetLoggerLevelDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(SET_LOGGER_LEVEL);
        node.get(DESCRIPTION).set(bundle.getString("logging.set-logger-level"));

        final ModelNode reqProps = node.get(REQUEST_PROPERTIES);
        addLoggerNameParam(reqProps, bundle);
        final ModelNode level = reqProps.get(LEVEL_NAME);
        level.get(DESCRIPTION).set(bundle.getString(LOGGING + "." + LEVEL_NAME));
        level.get(TYPE).set(ModelType.STRING);
        level.get(REQUIRED).set(false);

        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    public static ModelNode getGetParentLoggerNameDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(GET_PARENT_LOGGER_NAME);
        node.get(DESCRIPTION).set(bundle.getString("logging.get-parent-logger-name"));

        addLoggerNameParam(node.get(REQUEST_PROPERTIES), bundle);

        node.get(REPLY_PROPERTIES);
        node.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        node.get(REPLY_PROPERTIES, NILLABLE).set(true);

        return node;
    }

    private static void addLoggerNameParam(final ModelNode requestProperties, final ResourceBundle bundle) {
        final ModelNode param = requestProperties.get(LOGGER_NAME);
        param.get(DESCRIPTION).set(bundle.getString(LOGGING + "." + LOGGER_NAME));
        param.get(TYPE).set(ModelType.STRING);
        param.get(MIN_LENGTH).set(0);
        param.get(REQUIRED).set(true);
    }

    public static ModelNode getDescriptionOnlyOperation(final Locale locale, final String name, final String descriptionKeyBase) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(name);
        node.get(DESCRIPTION).set(bundle.getString(descriptionKeyBase + "." + name));

        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
