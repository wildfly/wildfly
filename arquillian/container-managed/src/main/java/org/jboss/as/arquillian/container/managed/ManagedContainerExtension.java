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
import org.jboss.arquillian.container.test.impl.enricher.resource.DeployerProvider;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.as.arquillian.container.CommonContainerExtension;

/**
 * The extensions used by the managed container.
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @author Thomas.Diesler@jboss.com
 * @since 02-Jun-2011
 */
public class ManagedContainerExtension extends CommonContainerExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        super.register(builder);
        builder.service(DeployableContainer.class, ManagedDeployableContainer.class);
        builder.override(ResourceProvider.class, DeployerProvider.class, ManagedDeployerProvider.class);
        builder.observer(ManagedClientDeployerCreator.class);
        builder.observer(ManagedContainerDeployController.class);
    }
}
