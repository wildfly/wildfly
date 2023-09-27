/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.controller.ModelVersion;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class Constants {

    static final ModelVersion ELYTRON_BY_DEFAULT_VERSION = ModelVersion.create(6, 0, 0);
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

    static final Boolean ELYTRON_MANAGED_SECURITY = Boolean.TRUE;

    static final String ELYTRON_ENABLED_NAME = "elytron-enabled";


}
