/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.connector.util.CopyOnWriteArrayListMultiMap;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
public final class ConfiguredAdaptersService implements Service<CopyOnWriteArrayListMultiMap<String, ServiceName>> {


    public static final AttachmentKey<CopyOnWriteArrayListMultiMap> ATTACHMENT_KEY = AttachmentKey
            .create(CopyOnWriteArrayListMultiMap.class);

    private final CopyOnWriteArrayListMultiMap<String, ServiceName> adapters = new CopyOnWriteArrayListMultiMap<>();

    @Override
    public CopyOnWriteArrayListMultiMap<String, ServiceName> getValue() throws IllegalStateException {
        return adapters;
    }


    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }
}
