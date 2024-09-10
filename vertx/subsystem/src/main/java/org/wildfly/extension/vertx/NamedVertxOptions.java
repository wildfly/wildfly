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

import io.vertx.core.VertxOptions;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class NamedVertxOptions {

  /** The name of the configured VertxOptions **/
  private final String name;

  /** The VertxOptions used to construct the Vertx instance **/
  private final VertxOptions vertxOptions;

  public NamedVertxOptions(String name, VertxOptions vertxOptions) {
    this.name = name;
    this.vertxOptions = vertxOptions;
  }

  public String getName() {
    return name;
  }

  public VertxOptions getVertxOptions() {
    return vertxOptions;
  }

}
