/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jberet.spi.ArtifactFactory;

/**
 * ArtifactFactory for Java EE runtime environment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface WildFlyArtifactFactory extends ArtifactFactory {

    /**
     * Creates a {@linkplain ContextHandle context handle} required for setting up the CDI context.
     *
     * @return the newly create context handle
     */
    ContextHandle createContextHandle();
}
