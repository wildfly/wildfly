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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.List;

public abstract class VertxAttributes {

    public static final SimpleAttributeDefinition OPTION_NAME = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_OPTION_NAME, ModelType.STRING)
        .setRequired(false)
        .setAllowExpression(true)
        .setRestartAllServices()
        .build();

    private static final List<AttributeDefinition> ATTRS = new ArrayList<>();
    static {
        ATTRS.add(OPTION_NAME);
    }

    static List<AttributeDefinition> getSimpleAttributes() {
        return ATTRS;
    }

}
