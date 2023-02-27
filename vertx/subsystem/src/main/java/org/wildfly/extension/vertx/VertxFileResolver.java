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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.vfs.VFS;
import org.wildfly.extension.vertx.deployment.VertxDeploymentsRegistry;
import org.wildfly.extension.vertx.logging.VertxLogger;

import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.file.impl.FileResolverImpl;
import io.vertx.core.spi.file.FileResolver;

/**
 * This is the FileResolver implementation that supports jboss-vfs by returning the physical file.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxFileResolver implements FileResolver {

  private static final String VERTX_CLASSLOADING_MULTIPLE = "vertx.classloading.multiple";
  private static final boolean MULTIPLE_CLASSLOADING_ENABLED = Boolean.getBoolean(VERTX_CLASSLOADING_MULTIPLE);
  public static boolean isMultipleClassLoadingEnabled() {
    return MULTIPLE_CLASSLOADING_ENABLED;
  }

  private final FileResolverImpl delegate;
  private final ServerEnvironment serverEnvironment;

  public VertxFileResolver(FileSystemOptions fileSystemOptions, ServerEnvironment serverEnvironment) {
    delegate = new FileResolverImpl(fileSystemOptions);
    this.serverEnvironment = serverEnvironment;
  }

  @Override
  public File resolveFile(String fileName) {
    if (fileName.contains("..")) {
      throw new IllegalArgumentException("Cannot resolve files with .. in the file name.");
    }
    File file = new File(fileName);
    boolean absolute = file.isAbsolute();
    if (absolute && !file.getAbsolutePath().startsWith(serverEnvironment.getServerConfigurationDir().getAbsolutePath())) {
      throw VertxLogger.VERTX_LOGGER.fileResolveNotAllowed(fileName);
    }
    if (file.exists()) {
      return file;
    }
    if (!absolute) {
      URL url;
      try {
        if (isMultipleClassLoadingEnabled()) {
          url = VertxDeploymentsRegistry.instance().loadResource(fileName);
          // in this mode, it won't delegate.
          if (url == null) return file;
        } else {
          ClassLoader cl = Thread.currentThread().getContextClassLoader();
          if (cl == null) {
            cl = getClass().getClassLoader();
          }
          url = cl.getResource(fileName);
        }
        if (url != null && url.getProtocol().equals("vfs")) {
          file = VFS.getChild(url.getPath()).getPhysicalFile();
          if (file != null && file.exists()) {
            return file;
          }
        }
      } catch (IOException e) {
        VertxLogger.VERTX_LOGGER.failedToResolveVFSFile(fileName, e);
      }
    }
    return delegate.resolveFile(fileName);
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

}
