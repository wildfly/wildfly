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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.NullConfigurationPersister;

/**
 * Constants used in this module.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PlatformMBeanConstants {

    // Base values
    public static final String CLASS_LOADING = "class-loading";
    public static final String COMPILATION = "compilation";
    public static final String GARBAGE_COLLECTOR = "garbage-collector";
    public static final String MEMORY_MANAGER = "memory-manager";
    public static final String MEMORY = "memory";
    public static final String MEMORY_POOL = "memory-pool";
    public static final String OPERATING_SYSTEM = "operating-system";
    public static final String RUNTIME = "runtime";
    public static final String THREADING = "threading";

    // JDK 7 additions
    public static final String BUFFER_POOL = "buffer-pool";
    public static final String LOGGING = "logging";

    public static final String PLATFORM_LOGGING_MXBEAN_NAME = "java.util.logging:type=Logging";
    public static final String BUFFER_POOL_MXBEAN_DOMAIN_TYPE = "java.nio:type=BufferPool";
    public static final ObjectName PLATFORM_LOGGING_OBJECT_NAME;

    public static final List<String> JDK_NOCOMPILATION_TYPES = Arrays.asList(CLASS_LOADING, GARBAGE_COLLECTOR, MEMORY_MANAGER, MEMORY, MEMORY_POOL, OPERATING_SYSTEM,
        RUNTIME, THREADING);
    private static final List<String> JDK6_BASE_TYPES = Arrays.asList(CLASS_LOADING, COMPILATION, GARBAGE_COLLECTOR, MEMORY_MANAGER, MEMORY, MEMORY_POOL, OPERATING_SYSTEM,
        RUNTIME, THREADING);

    public static final List<String> BASE_TYPES;

    public static final String OBJECT_NAME = "object-name";

    // ClassLoadingMXBean
    public static final String TOTAL_LOADED_CLASS_COUNT = "total-loaded-class-count";
    public static final String LOADED_CLASS_COUNT = "loaded-class-count";
    public static final String UNLOADED_CLASS_COUNT = "unloaded-class-count";
    public static final String VERBOSE = "verbose";

    public static final List<String> CLASSLOADING_METRICS = Arrays.asList(
        TOTAL_LOADED_CLASS_COUNT, LOADED_CLASS_COUNT, UNLOADED_CLASS_COUNT
    );

    public static final List<String> CLASSLOADING_READ_WRITE_ATTRIBUTES = Arrays.asList(
        VERBOSE
    );

    // CompilationMXBean
    public static final String COMPILATION_TIME_MONITORING_SUPPORTED = "compilation-time-monitoring-supported";
    public static final String TOTAL_COMPILATION_TIME = "total-compilation-time";

    public static final List<String> COMPILATION_READ_ATTRIBUTES = Arrays.asList(
        ModelDescriptionConstants.NAME, COMPILATION_TIME_MONITORING_SUPPORTED
    );

    public static final List<String> COMPILATION_METRICS = Arrays.asList(
        TOTAL_COMPILATION_TIME
    );

    // GarbageCollectorMXBean
    public static final String VALID = "valid";
    public static final String MEMORY_POOL_NAMES = "memory-pool-names";
    public static final String COLLECTION_COUNT = "collection-count";
    public static final String COLLECTION_TIME = "collection-time";

    public static final List<String> GARBAGE_COLLECTOR_READ_ATTRIBUTES = Arrays.asList(
        ModelDescriptionConstants.NAME, VALID, MEMORY_POOL_NAMES
    );

    public static final List<String> GARBAGE_COLLECTOR_METRICS = Arrays.asList(
        COLLECTION_COUNT, COLLECTION_TIME
    );

    // MemoryMXBean
    public static final String OBJECT_PENDING_FINALIZATION_COUNT = "object-pending-finalization-count";
    public static final String HEAP_MEMORY_USAGE = "heap-memory-usage";
    public static final String NON_HEAP_MEMORY_USAGE = "non-heap-memory-usage";
    public static final String GC = "gc";

    public static final String INIT = "init";
    public static final String USED = "used";
    public static final String COMMITTED = "committed";
    public static final String MAX = "max";


    public static final List<String> MEMORY_METRICS = Arrays.asList(
        OBJECT_PENDING_FINALIZATION_COUNT, HEAP_MEMORY_USAGE, NON_HEAP_MEMORY_USAGE
    );

    public static final List<String> MEMORY_READ_WRITE_ATTRIBUTES = Arrays.asList(
        VERBOSE
    );

    // MemoryManagerMXBean

    public static final List<String> MEMORY_MANAGER_READ_ATTRIBUTES = Arrays.asList(
        ModelDescriptionConstants.NAME, VALID, MEMORY_POOL_NAMES
    );

    // MemoryPoolMXBean
    public static final String TYPE = "type";
    public static final String USAGE = "usage";
    public static final String PEAK_USAGE = "peak-usage";
    public static final String MEMORY_MANAGER_NAMES = "memory-manager-names";
    public static final String USAGE_THRESHOLD = "usage-threshold";
    public static final String USAGE_THRESHOLD_EXCEEDED = "usage-threshold-exceeded";
    public static final String USAGE_THRESHOLD_COUNT = "usage-threshold-count";
    public static final String USAGE_THRESHOLD_SUPPORTED = "usage-threshold-supported";
    public static final String COLLECTION_USAGE_THRESHOLD = "collection-usage-threshold";
    public static final String COLLECTION_USAGE_THRESHOLD_EXCEEDED = "collection-usage-threshold-exceeded";
    public static final String COLLECTION_USAGE_THRESHOLD_COUNT = "collection-usage-threshold-count";
    public static final String COLLECTION_USAGE = "collection-usage";
    public static final String COLLECTION_USAGE_THRESHOLD_SUPPORTED = "collection-usage-threshold-supported";

    public static final String RESET_PEAK_USAGE = "reset-peak-usage";

    public static final List<String> MEMORY_POOL_READ_ATTRIBUTES = Arrays.asList(
        ModelDescriptionConstants.NAME, TYPE, VALID, MEMORY_MANAGER_NAMES, USAGE_THRESHOLD_SUPPORTED,
        COLLECTION_USAGE_THRESHOLD_SUPPORTED
    );

    public static final List<String> MEMORY_POOL_METRICS = Arrays.asList(
        USAGE, PEAK_USAGE, USAGE_THRESHOLD_EXCEEDED, USAGE_THRESHOLD_COUNT, COLLECTION_USAGE_THRESHOLD_EXCEEDED,
        COLLECTION_USAGE_THRESHOLD_COUNT, COLLECTION_USAGE
    );

    public static final List<String> MEMORY_POOL_READ_WRITE_ATTRIBUTES = Arrays.asList(
        USAGE_THRESHOLD, COLLECTION_USAGE_THRESHOLD
    );


    // OperatingSystemMXBean
    public static final String ARCH = "arch";
    public static final String VERSION = "version";
    public static final String AVAILABLE_PROCESSORS = "available-processors";
    public static final String SYSTEM_LOAD_AVERAGE = "system-load-average";

    public static final List<String> OPERATING_SYSTEM_READ_ATTRIBUTES = Arrays.asList(
        ModelDescriptionConstants.NAME, ARCH, VERSION
    );

    public static final List<String> OPERATING_SYSTEM_METRICS = Arrays.asList(
        AVAILABLE_PROCESSORS, SYSTEM_LOAD_AVERAGE
    );

    // RuntimeMXBean
    public static final String VM_NAME = "vm-name";
    public static final String VM_VENDOR = "vm-vendor";
    public static final String VM_VERSION = "vm-version";
    public static final String SPEC_NAME = "spec-name";
    public static final String SPEC_VENDOR = "spec-vendor";
    public static final String SPEC_VERSION = "spec-version";
    public static final String MANAGEMENT_SPEC_VERSION = "management-spec-version";
    public static final String CLASS_PATH = "class-path";
    public static final String LIBRARY_PATH = "library-path";
    public static final String BOOT_CLASS_PATH_SUPPORTED = "boot-class-path-supported";
    public static final String BOOT_CLASS_PATH = "boot-class-path";
    public static final String INPUT_ARGUMENTS = "input-arguments";
    public static final String UPTIME = "uptime";
    public static final String START_TIME = "start-time";
    public static final String SYSTEM_PROPERTIES = "system-properties";

    public static final List<String> RUNTIME_READ_ATTRIBUTES = Arrays.asList(
        ModelDescriptionConstants.NAME, VM_NAME, VM_VENDOR, VM_VERSION, SPEC_NAME, SPEC_VENDOR, SPEC_VERSION,
        MANAGEMENT_SPEC_VERSION, CLASS_PATH, LIBRARY_PATH, BOOT_CLASS_PATH_SUPPORTED, BOOT_CLASS_PATH,
        INPUT_ARGUMENTS, START_TIME, SYSTEM_PROPERTIES
    );

    public static final List<String> RUNTIME_METRICS = Arrays.asList(
        UPTIME
    );


    // ThreadMXBean
    public static final String THREAD_COUNT = "thread-count";
    public static final String PEAK_THREAD_COUNT = "peak-thread-count";
    public static final String TOTAL_STARTED_THREAD_COUNT = "total-started-thread-count";
    public static final String DAEMON_THREAD_COUNT = "daemon-thread-count";
    public static final String ALL_THREAD_IDS = "all-thread-ids";
    public static final String THREAD_CONTENTION_MONITORING_SUPPORTED = "thread-contention-monitoring-supported";
    public static final String THREAD_CONTENTION_MONITORING_ENABLED = "thread-contention-monitoring-enabled";
    public static final String CURRENT_THREAD_CPU_TIME = "current-thread-cpu-time";
    public static final String CURRENT_THREAD_USER_TIME = "current-thread-user-time";
    public static final String THREAD_CPU_TIME_SUPPORTED = "thread-cpu-time-supported";
    public static final String CURRENT_THREAD_CPU_TIME_SUPPORTED = "current-thread-cpu-time-supported";
    public static final String THREAD_CPU_TIME_ENABLED = "thread-cpu-time-enabled";
    public static final String OBJECT_MONITOR_USAGE_SUPPORTED = "object-monitor-usage-supported";
    public static final String SYNCHRONIZER_USAGE_SUPPORTED = "synchronizer-usage-supported";

    public static final String RESET_PEAK_THREAD_COUNT = "reset-peak-thread-count";
    public static final String FIND_DEADLOCKED_THREADS = "find-deadlocked-threads";
    public static final String FIND_MONITOR_DEADLOCKED_THREADS = "find-monitor-deadlocked-threads";
    public static final String GET_THREAD_INFO = "get-thread-info";
    public static final String GET_THREAD_INFOS = "get-thread-infos";
    public static final String GET_THREAD_CPU_TIME = "get-thread-cpu-time";
    public static final String GET_THREAD_USER_TIME = "get-thread-user-time";
    public static final String DUMP_ALL_THREADS = "dump-all-threads";
    public static final String ID = "id";
    public static final String IDS = "ids";
    public static final String MAX_DEPTH = "max-depth";
    public static final String LOCKED_MONITORS = "locked-monitors";
    public static final String LOCKED_SYNCHRONIZERS = "locked-synchronizers";

    public static final String FILE_NAME = "file-name";
    public static final String LINE_NUMBER = "line-number";
    public static final String CLASS_NAME = "class-name";
    public static final String METHOD_NAME = "method-name";
    public static final String NATIVE_METHOD = "native-method";

    public static final String THREAD_ID = "thread-id";
    public static final String THREAD_NAME = "thread-name";
    public static final String THREAD_STATE = "thread-state";
    public static final String BLOCKED_TIME = "blocked-time";
    public static final String BLOCKED_COUNT = "blocked-count";
    public static final String WAITED_TIME = "waited-time";
    public static final String WAITED_COUNT = "waited-count";
    public static final String LOCK_INFO = "lock-info";
    public static final String LOCK_NAME = "lock-name";
    public static final String LOCK_OWNER_ID = "lock-owner-id";
    public static final String LOCK_OWNER_NAME = "lock-owner-name";
    public static final String STACK_TRACE = "stack-trace";
    public static final String SUSPENDED = "suspended";
    public static final String IN_NATIVE = "in-native";

    public static final String IDENTITY_HASH_CODE = "identity-hash-code";
    public static final String LOCKED_STACK_DEPTH = "locked-stack-depth";
    public static final String LOCKED_STACK_FRAME = "locked-stack-frame";

    public static final List<String> THREADING_READ_ATTRIBUTES = Arrays.asList(
        ALL_THREAD_IDS, THREAD_CONTENTION_MONITORING_SUPPORTED, THREAD_CPU_TIME_SUPPORTED, CURRENT_THREAD_CPU_TIME_SUPPORTED,
        OBJECT_MONITOR_USAGE_SUPPORTED, SYNCHRONIZER_USAGE_SUPPORTED
    );

    public static final List<String> THREADING_METRICS = Arrays.asList(
        THREAD_COUNT, PEAK_THREAD_COUNT, TOTAL_STARTED_THREAD_COUNT, DAEMON_THREAD_COUNT,
        CURRENT_THREAD_CPU_TIME, CURRENT_THREAD_USER_TIME
    );

    public static final List<String> THREADING_READ_WRITE_ATTRIBUTES = Arrays.asList(
        THREAD_CONTENTION_MONITORING_ENABLED, THREAD_CPU_TIME_ENABLED
    );

    // BufferPoolMXBean
    public static final String COUNT = "count";
    public static final String MEMORY_USED = "memory-used";
    public static final String TOTAL_CAPACITY = "total-capacity";

    public static final List<String> BUFFER_POOL_READ_ATTRIBUTES = Arrays.asList(
        ModelDescriptionConstants.NAME
    );

    public static final List<String> BUFFER_POOL_METRICS = Arrays.asList(
        COUNT, MEMORY_USED, TOTAL_CAPACITY
    );

    // PlatformLoggingMXBean

    public static final String LOGGER_NAMES = "logger-names";

    public static final List<String> LOGGING_READ_ATTRIBUTES = Arrays.asList(
        LOGGER_NAMES
    );



    public static final String GET_LOGGER_LEVEL = "get-logger-level";
    public static final String SET_LOGGER_LEVEL = "set-logger-level";
    public static final String GET_PARENT_LOGGER_NAME = "get-parent-logger-name";
    public static final String LOGGER_NAME = "logger-name";
    public static final String LEVEL_NAME = "level-name";


    // Paths
    public static final PathElement ROOT_PATH = PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE,
                                                                       ModelDescriptionConstants.PLATFORM_MBEAN);

    public static final PathElement CLASS_LOADING_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, CLASS_LOADING);
    public static final PathElement COMPILATION_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, COMPILATION);
    public static final PathElement GARBAGE_COLLECTOR_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, GARBAGE_COLLECTOR);
    public static final PathElement MEMORY_MANAGER_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, MEMORY_MANAGER);
    public static final PathElement MEMORY_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, MEMORY);
    public static final PathElement MEMORY_POOL_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, MEMORY_POOL);
    public static final PathElement OPERATING_SYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, OPERATING_SYSTEM);
    public static final PathElement RUNTIME_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, RUNTIME);
    public static final PathElement THREADING_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, THREADING);
    public static final PathElement BUFFER_POOL_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, BUFFER_POOL);
    public static final PathElement LOGGING_PATH = PathElement.pathElement(ModelDescriptionConstants.TYPE, LOGGING);


    static {
        final List<String> JDK6 = ManagementFactory.getCompilationMXBean() == null ? JDK_NOCOMPILATION_TYPES : JDK6_BASE_TYPES;
        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            List<String> list = new ArrayList<String>(JDK6);
            list.add(BUFFER_POOL);
            list.add(LOGGING);
            BASE_TYPES = Collections.unmodifiableList(list);
        } else {
            BASE_TYPES = JDK6;
        }

        try {
            PLATFORM_LOGGING_OBJECT_NAME = new ObjectName(PLATFORM_LOGGING_MXBEAN_NAME);
        } catch (MalformedObjectNameException e) {
            // Shouldn't happen; just satisfy compiler
            throw new IllegalStateException(PLATFORM_LOGGING_MXBEAN_NAME + " somehow isn't a legal object name???");
        }
    }

    private PlatformMBeanConstants() {
        // prevent instantiation
    }
}
