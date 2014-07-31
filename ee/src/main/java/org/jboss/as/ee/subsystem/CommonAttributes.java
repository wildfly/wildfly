/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.ee.subsystem;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public interface CommonAttributes {

    String CONTEXT_SERVICE = "context-service";
    String CORE_THREADS = "core-threads";
    String HUNG_TASK_THRESHOLD = "hung-task-threshold";
    String JNDI_NAME = "jndi-name";
    String KEEPALIVE_TIME = "keepalive-time";
    String LONG_RUNNING_TASKS = "long-running-tasks";
    String MAX_THREADS = "max-threads";
    String PRIORITY = "priority";
    String QUEUE_LENGTH = "queue-length";
    String REJECT_POLICY = "reject-policy";
    String THREAD_FACTORY = "thread-factory";
    String TIME = "time";
    String UNIT = "unit";
}
