/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jberet.spi.ArtifactFactory;

/**
 * ArtifactFactory for Jakarta EE runtime environment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface WildFlyArtifactFactory extends ArtifactFactory {

    /**
     * Creates a {@linkplain ContextHandle context handle} required for setting up the Jakarta Contexts and Dependency Injection context.
     *
     * @return the newly create context handle
     */
    ContextHandle createContextHandle();
}
