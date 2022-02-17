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

import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;

import static org.wildfly.extension.vertx.VertxResourceDefinition.*;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
abstract class AbstractVertxOptionsResourceDefinition extends PersistentResourceDefinition {

  static final RuntimeCapability<Void> VERTX_OPTIONS_CAPABILITY =
    RuntimeCapability.Builder.of(VERTX_CAPABILITY_NAME + ".options", true, NamedVertxOptions.class)
      .setDynamicNameMapper(DynamicNameMappers.PARENT)
      .build();

  protected AbstractVertxOptionsResourceDefinition(SimpleResourceDefinition.Parameters parameters) {
    super(parameters);
  }

}
