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
public class Constants {
    static final String JCA = "jca";

    static final String ARCHIVE_VALIDATION = "archive-validation";

    static final String BEAN_VALIDATION = "bean-validation";

    static final String TRACER = "tracer";

    static final String CACHED_CONNECTION_MANAGER = "cached-connection-manager";

    public static final String DEFAULT_NAME = "default";
    static final String WORKMANAGER_SHORT_RUNNING = "short-running-threads";
    static final String WORKMANAGER_LONG_RUNNING = "long-running-threads";

    static final String WORKMANAGER = "workmanager";

    static final String DISTRIBUTED_WORKMANAGER = "distributed-workmanager";


    static final String BOOTSTRAP_CONTEXT = "bootstrap-context";

    static final String TX = "TX";
    static final String NON_TX = "NonTX";

    static final Boolean ELYTRON_MANAGED_SECURITY = Boolean.FALSE;

    static final String ELYTRON_ENABLED_NAME = "elytron-enabled";


}
