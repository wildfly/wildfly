/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.managed;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.as.arquillian.container.DelegatingApplicationArchiveProcessor;
import org.jboss.as.arquillian.container.JBossASExceptionTransformer;
import org.jboss.as.arquillian.protocol.jmx.JBossASProtocol;

/**
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
public class JBossAsManagedContainerExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(Protocol.class, JBossASProtocol.class);
        builder.service(DeployableContainer.class, JBossAsManagedContainer.class);
        builder.service(ApplicationArchiveProcessor.class, DelegatingApplicationArchiveProcessor.class);
        builder.service(DeploymentExceptionTransformer.class, JBossASExceptionTransformer.class);
    }
}
