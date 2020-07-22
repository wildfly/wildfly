/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.vertx;

//import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemRootResourceDefinition extends PersistentResourceDefinition {
    static final String VERTX_CAPABILITY_NAME = "org.wildfly.extension.vertx";

    static final RuntimeCapability<Void> VERTX_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(VERTX_CAPABILITY_NAME, VertxProxy.class)
                    .build();

    VertxSubsystemRootResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(VertxSubsystemExtension.SUBSYSTEM_PATH,
                VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, "root"))
                .setAddHandler(VertxSubsystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(VERTX_RUNTIME_CAPABILITY)
        );
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        //you can register aditional operations here
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        //you can register attributes here
//        for (AttributeDefinition att: getAttributes()) {
//            resourceRegistration.registerReadWriteAttribute(attr, );
//        }
        super.registerAttributes(resourceRegistration);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }
}
