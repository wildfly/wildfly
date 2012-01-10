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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Utilities for working with platform mbeans.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PlatformMBeanUtil {

    public static final int JVM_MAJOR_VERSION;

    static {
        int vmVersion;
        try {
            String vmVersionStr = SecurityActions.getSystemProperty("java.specification.version");
            vmVersion = Integer.valueOf(vmVersionStr.substring(2));
        } catch (Exception e) {
            vmVersion = 6;
        }
        JVM_MAJOR_VERSION = vmVersion;
    }

    public static String escapeMBeanName(final String toEscape) {
        return toEscape.replace(' ', '_');
    }

    public static String unescapeMBeanValue(final String toUnescape) {
        String unescaped = toUnescape.replace('_', ' ');
        return unescaped.equals(toUnescape) ? toUnescape : "\"" + unescaped + "\"";
    }

    public static String getObjectNameStringWithNameKey(final String base, final String escapedValue) {
        final String value = unescapeMBeanValue(escapedValue);
        return base + ",name=" + value;
    }

    public static ObjectName getObjectNameWithNameKey(final String base, final String escapedValue) throws OperationFailedException {
        try {
            return new ObjectName(getObjectNameStringWithNameKey(base, escapedValue));
        } catch (MalformedObjectNameException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }
    }

    public static Object getMBeanAttribute(final ObjectName objectName, final String attribute) throws OperationFailedException {
        try {
            return ManagementFactory.getPlatformMBeanServer().getAttribute(objectName, attribute);
        } catch (ReflectionException e) {
            Throwable t = e.getTargetException();
            if (t instanceof SecurityException || t instanceof UnsupportedOperationException) {
                throw new OperationFailedException(new ModelNode().set(t.toString()));
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility for converting {@link java.lang.management.MemoryUsage} to a detyped form.
     * @param memoryUsage the memory usage data object
     * @return the detyped representation
     */
    public static ModelNode getDetypedMemoryUsage(final MemoryUsage memoryUsage) {
        final ModelNode result = new ModelNode();
        if (memoryUsage != null) {
            result.get(PlatformMBeanConstants.INIT).set(memoryUsage.getInit());
            result.get(PlatformMBeanConstants.USED).set(memoryUsage.getUsed());
            result.get(PlatformMBeanConstants.COMMITTED).set(memoryUsage.getCommitted());
            result.get(PlatformMBeanConstants.MAX).set(memoryUsage.getMax());
        }
        return result;
    }

    /**
     * Utility for converting {@link java.lang.management.ThreadInfo} to a detyped form.
     *
     * @param threadInfo the thread information data object
     * @param includeBlockedTime whether the {@link PlatformMBeanConstants#BLOCKED_TIME} attribute is supported
     * @return the detyped representation
     */
    public static ModelNode getDetypedThreadInfo(final ThreadInfo threadInfo, boolean includeBlockedTime) {
        final ModelNode result = new ModelNode();

        result.get(PlatformMBeanConstants.THREAD_ID).set(threadInfo.getThreadId());
        result.get(PlatformMBeanConstants.THREAD_NAME).set(threadInfo.getThreadName());
        result.get(PlatformMBeanConstants.THREAD_STATE).set(threadInfo.getThreadState().name());
        if (includeBlockedTime) {
            result.get(PlatformMBeanConstants.BLOCKED_TIME).set(threadInfo.getBlockedTime());
        } else {
            result.get(PlatformMBeanConstants.BLOCKED_TIME);
        }
        result.get(PlatformMBeanConstants.BLOCKED_COUNT).set(threadInfo.getBlockedCount());
        result.get(PlatformMBeanConstants.WAITED_TIME).set(threadInfo.getWaitedTime());
        result.get(PlatformMBeanConstants.WAITED_COUNT).set(threadInfo.getWaitedCount());
        result.get(PlatformMBeanConstants.LOCK_INFO).set(getDetypedLockInfo(threadInfo.getLockInfo()));
        nullSafeSet(result.get(PlatformMBeanConstants.LOCK_NAME), threadInfo.getLockName());
        result.get(PlatformMBeanConstants.LOCK_OWNER_ID).set(threadInfo.getLockOwnerId());
        nullSafeSet(result.get(PlatformMBeanConstants.LOCK_OWNER_NAME), threadInfo.getLockOwnerName());
        final ModelNode stack = result.get(PlatformMBeanConstants.STACK_TRACE);
        stack.setEmptyList();
        for (StackTraceElement ste : threadInfo.getStackTrace()) {
            stack.add(getDetypedStackTraceElement(ste));
        }
        result.get(PlatformMBeanConstants.SUSPENDED).set(threadInfo.isSuspended());
        result.get(PlatformMBeanConstants.IN_NATIVE).set(threadInfo.isInNative());
        final ModelNode monitors = result.get(PlatformMBeanConstants.LOCKED_MONITORS);
        monitors.setEmptyList();
        for (MonitorInfo monitor : threadInfo.getLockedMonitors()) {
            monitors.add(getDetypedMonitorInfo(monitor));
        }
        final ModelNode synchronizers = result.get(PlatformMBeanConstants.LOCKED_SYNCHRONIZERS);
        synchronizers.setEmptyList();
        for (LockInfo lock : threadInfo.getLockedSynchronizers()) {
            synchronizers.add(getDetypedLockInfo(lock));
        }

        return result;
    }

    private static void nullSafeSet(final ModelNode node, final String value) {
        if (value != null) {
            node.set(value);
        }
    }

    private static ModelNode getDetypedLockInfo(final LockInfo lockInfo) {
        final ModelNode result = new ModelNode();
        if (lockInfo != null) {
            result.get(PlatformMBeanConstants.CLASS_NAME).set(lockInfo.getClassName());
            result.get(PlatformMBeanConstants.IDENTITY_HASH_CODE).set(lockInfo.getIdentityHashCode());
        }
        return result;
    }

    private static ModelNode getDetypedMonitorInfo(final MonitorInfo monitorInfo) {
        final ModelNode result = getDetypedLockInfo(monitorInfo);
        if (monitorInfo != null) {
            result.get(PlatformMBeanConstants.LOCKED_STACK_DEPTH).set(monitorInfo.getLockedStackDepth());
            final ModelNode frame = getDetypedStackTraceElement(monitorInfo.getLockedStackFrame());
            result.get(PlatformMBeanConstants.LOCKED_STACK_FRAME).set(frame);
        }
        return result;
    }

    private static ModelNode getDetypedStackTraceElement(final StackTraceElement stackTraceElement) {
        final ModelNode result = new ModelNode();
        if (stackTraceElement != null) {
            result.get(PlatformMBeanConstants.FILE_NAME).set(stackTraceElement.getFileName());
            result.get(PlatformMBeanConstants.LINE_NUMBER).set(stackTraceElement.getLineNumber());
            result.get(PlatformMBeanConstants.CLASS_NAME).set(stackTraceElement.getClassName());
            result.get(PlatformMBeanConstants.METHOD_NAME).set(stackTraceElement.getMethodName());
            result.get(PlatformMBeanConstants.NATIVE_METHOD).set(stackTraceElement.isNativeMethod());
        }
        return result;
    }

    private PlatformMBeanUtil() {

    }
}
