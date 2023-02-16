/*
 *  Copyright (c) 2023 The original author or authors
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

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProxyHolder {
  private static final VertxProxyHolder INSTANCE = new VertxProxyHolder();

  public static VertxProxyHolder instance() {
    return INSTANCE;
  }

  private final AtomicReference<VertxProxy> vertxProxyRef = new AtomicReference<>();

  private VertxProxyHolder() {}

  void instrument(VertxProxy vertxProxy) {
    this.vertxProxyRef.set(vertxProxy);
  }

  void release() {
    this.vertxProxyRef.set(null);
  }

  public VertxProxy getVertxProxy() {
    return this.vertxProxyRef.get();
  }

}
