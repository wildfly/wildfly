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

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
class Constants {
    static final String JCA = "jca";

    static final String ARCHIVE_VALIDATION = "archive-validation";
    static final String ARCHIVE_VALIDATION_ENABLED = "enabled";
    static final String ARCHIVE_VALIDATION_FAIL_ON_ERROR = "fail-on-error";
    static final String ARCHIVE_VALIDATION_FAIL_ON_WARN = "fail-on-warn";

    static final String BEAN_VALIDATION = "bean-validation";
    static final String BEAN_VALIDATION_ENABLED = "enabled";

    static final String CACHED_CONNECTION_MANAGER = "cached-connection-manager";
    static final String CACHED_CONNECTION_MANAGER_DEBUG = "debug";
    static final String CACHED_CONNECTION_MANAGER_ERROR = "error";

    static final String DEFAULT_NAME = "default";
    static final String THREAD_POOL = "thread-pool";
    static final String WORKMANAGER_SHORT_RUNNING = "short-running-threads";
    static final String WORKMANAGER_LONG_RUNNING = "long-running-threads";

    static final String WORKMANAGERS = "workmanagers";

    static final String WORKMANAGER = "workmanager";
    static final String WORKMANAGER_NAME = "name";

    static final String BOOTSTRAP_CONTEXTS = "bootstrap-contexts";

    static final String BOOTSTRAP_CONTEXT = "bootstrap-context";
    static final String BOOTSTRAP_CONTEXT_WORKMANAGER = "workmanager";
}
