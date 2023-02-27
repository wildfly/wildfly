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
package org.wildfly.extension.vertx.deployment;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxDeploymentsRegistry {
  private static final VertxDeploymentsRegistry INSTANCE = new VertxDeploymentsRegistry();
  public static VertxDeploymentsRegistry instance() {
    return INSTANCE;
  }

  private static final ConcurrentHashMap<String, VertxDeployment> vertxDeployments = new ConcurrentHashMap<>();
  public synchronized URL loadResource(String resource) throws IOException {
    for (VertxDeployment vertxDeployment: vertxDeployments.values()) {
      URL url = vertxDeployment.loadResource(resource);
      if (url != null) return url;
    }
    return null;
  }

  public VertxDeployment vertxDeployment(String moduleName) {
    return vertxDeployments.get(moduleName);
  }

  public void register(VertxDeployment vertxDeployment) {
    vertxDeployments.putIfAbsent(vertxDeployment.getName(), vertxDeployment);
  }

  public void unregister(String moduleName) {
    vertxDeployments.remove(moduleName);
  }

}
