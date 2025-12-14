/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.cdi.remote.deployment;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_PASSWORD;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_USER;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_ADDRESS;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_PORT;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;


/**
 * This is the CDI configuration class for remote caches.
 *
 * @author Radoslav Husar
 * @since 27
 */
public class RemoteCdiConfig {

    @Produces
    @ApplicationScoped
    @RemoteGreetingCache
    public static RemoteCacheManager defaultRemoteCacheManager() {
        Configuration configuration = new ConfigurationBuilder()
                .addServer().host(INFINISPAN_SERVER_ADDRESS).port(INFINISPAN_SERVER_PORT)
                .security().authentication().username(INFINISPAN_APPLICATION_USER).password(INFINISPAN_APPLICATION_PASSWORD)
                .build();
        return new RemoteCacheManager(configuration);
    }

    static void stopRemoteCacheManager(@Disposes @Any RemoteCacheManager remoteCacheManager) {
        remoteCacheManager.close();
    }
}