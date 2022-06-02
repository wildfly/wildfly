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

import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.file.impl.FileResolverImpl;
import io.vertx.core.spi.file.FileResolver;
import org.jboss.vfs.VFS;
import org.wildfly.extension.vertx.logging.VertxLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * This is the FileResolver implementation that supports jboss-vfs by returning the physical file.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxFileResolver implements FileResolver {
  private final FileResolverImpl delegate;

  public VertxFileResolver() {
    this(new FileSystemOptions());
  }

  public VertxFileResolver(FileSystemOptions fileSystemOptions) {
    delegate = new FileResolverImpl(fileSystemOptions);
  }

  @Override
  public File resolveFile(String fileName) {
    File file = new File(fileName);
    boolean absolute = file.isAbsolute();
    if (file.exists()) {
      return file;
    }
    if (!absolute) {
      URL url = getClassLoader().getResource(fileName);
      if (url != null && url.getProtocol().equals("vfs")) {
        try {
          file = VFS.getChild(url.getPath()).getPhysicalFile();
          if (file != null && file.exists()) {
            return file;
          }
        } catch (IOException e) {
          VertxLogger.VERTX_LOGGER.failedToResolveVFSFile(fileName, e);
        }
      }
    }
    return delegate.resolveFile(fileName);
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  private ClassLoader getClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = getClass().getClassLoader();
    }
    // when running on substratevm (graal) the access to class loaders
    // is very limited and might be only available from compile time
    // known classes. (Object is always known, so we do a final attempt
    // to get it here).
    if (cl == null) {
      cl = Object.class.getClassLoader();
    }
    return cl;
  }

}
