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

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.modules.ModuleClassLoader;

import io.vertx.core.Vertx;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxDeployment {
  private final ModuleClassLoader classLoader;
  private final boolean expose;
  private final List<String> exposedResources;
  private final Set<String> verticleDeployments = new HashSet<>();

  public VertxDeployment (ModuleClassLoader classLoader, boolean expose, List<String> exposedResources) {
    this.classLoader = classLoader;
    this.expose = expose;
    this.exposedResources = exposedResources;
  }

  public synchronized void verticleDeployed(String deploymentId) {
    verticleDeployments.add(deploymentId);
  }

  public synchronized void verticleUnDeployed(String deploymentId) {
    verticleDeployments.remove(deploymentId);
  }

  public String getName() {
    return this.classLoader.getModule().getName();
  }

  public URL loadResource(String resource) {
    URL url = null;
    synchronized (this) {
      String currentDeploymentId = Vertx.currentContext() == null ? null : Vertx.currentContext().deploymentID();
      if (currentDeploymentId != null) {
        if (verticleDeployments.contains(currentDeploymentId)) {
          url = classLoader.getResource(resource);
        }
      }
    }
    if (url != null) return url;
    if (!expose || this.exposedResources.isEmpty() || !exposed("/" + resource)) {
      return null;
    }
    return classLoader.getResource(resource);
  }

  // check if resource is exposed or not, return true if exposed
  private boolean exposed(String resource) {
    for (String exposed: exposedResources) {
      String res = exposed.startsWith("/") ? exposed : "/" + exposed;
      if (resource.equals(res)) {
        return true;
      }
      if (res.endsWith("*") && resource.startsWith(res.substring(0, res.length() - 1))) {
        return true;
      }
    }
    return false;
  }

}
