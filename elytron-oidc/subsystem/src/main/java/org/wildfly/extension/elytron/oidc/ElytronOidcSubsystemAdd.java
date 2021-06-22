/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger;

/**
 * Add handler for the Elytron OpenID Connect subsystem.
 *
 * <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class ElytronOidcSubsystemAdd extends AbstractBoottimeAddStepHandler {

    ElytronOidcSubsystemAdd() {
    }

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {
        ElytronOidcLogger.ROOT_LOGGER.activatingSubsystem();
    }
}
