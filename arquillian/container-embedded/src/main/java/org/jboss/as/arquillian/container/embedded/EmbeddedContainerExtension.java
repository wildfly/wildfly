/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.arquillian.container.embedded;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.as.arquillian.container.CommonContainerExtension;

/**
 * The extensions used by the managed container.
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author <a href="mailto:mmatloka@gmail.com">Michal Matloka</a>
 * @since 21-Sep-2012
 */
public class EmbeddedContainerExtension extends CommonContainerExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        super.register(builder);
        builder.service(DeployableContainer.class, EmbeddedDeployableContainer.class);
    }
}