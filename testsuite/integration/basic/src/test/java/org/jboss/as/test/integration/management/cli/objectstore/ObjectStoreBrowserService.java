/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.cli.objectstore;

import com.arjuna.ats.arjuna.tools.osb.api.mbeans.RecoveryStoreBean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * Instantiates RecoveryStoreBean on JMX.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@Singleton
@Startup
public class ObjectStoreBrowserService {

    private RecoveryStoreBean rsb;

    @PostConstruct
    public void start() {
        rsb = new RecoveryStoreBean();
        rsb.start();
    }

    @PreDestroy
    public void stop() {
        if (rsb != null)
            rsb.stop();
    }

}
