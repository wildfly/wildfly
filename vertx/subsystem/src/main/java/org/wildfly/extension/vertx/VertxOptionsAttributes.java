/*
 *  Copyright (c) 2022 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.wildfly.extension.vertx;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public abstract class VertxOptionsAttributes {

  public static final SimpleAttributeDefinition VERTX_OPTION_FILE_PATH = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_PATH, ModelType.STRING)
    .setRequired(true)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  private static final List<AttributeDefinition> VERTX_OPTIONS_FILE_ATTRS = new ArrayList<>();
  static {
    VERTX_OPTIONS_FILE_ATTRS.add(VERTX_OPTION_FILE_PATH);
  }

  static List<AttributeDefinition> getVertxOptionsFileAttributes() {
    return VERTX_OPTIONS_FILE_ATTRS;
  }

}
