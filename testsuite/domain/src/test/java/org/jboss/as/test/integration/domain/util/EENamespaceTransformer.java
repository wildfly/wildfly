/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright $year Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.jboss.as.test.integration.domain.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.exporter.ArchiveExportException;
import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.impl.base.io.IOUtil;
import org.wildfly.galleon.plugin.transformer.JakartaTransformer;

public class EENamespaceTransformer {
    private static final Logger logger = Logger.getLogger(EENamespaceTransformer.class.getPackage().getName());

    private static final boolean TRANSFORM = System.getProperty("ts.ee9") != null
            || System.getProperty("ts.bootable.ee9") != null;

    public static void jakartaTransform(StreamExporter exporter, File target) {
        assert target != null;
        assert !target.isDirectory();
        if (TRANSFORM) {
            try (OutputStream out = new FileOutputStream(target)) {
                try (InputStream transformed = jakartaTransform(exporter.exportAsInputStream(), target.getName())) {
                    IOUtil.copy(transformed, out);
                } catch (final IOException e) {
                    throw new ArchiveExportException("Error encountered in exporting archive to " + target, e);
                }
            } catch (final FileNotFoundException e) {
                throw new ArchiveExportException("File could not be created: " + target);
            } catch (IOException e) {
                throw new ArchiveExportException("Error encountered in exporting archive to " + target, e);
            }
        } else {
            exporter.exportTo(target, true);
        }
    }

    public static InputStream jakartaTransform(InputStream source, String name) throws IOException {
        if (TRANSFORM) {
            final boolean verbose = logger.isTraceEnabled();
            return JakartaTransformer.transform(null, source, name, verbose, new JakartaTransformer.LogHandler() {
                @Override
                public void print(String format, Object... args) {
                    logger.tracef(format, args);
                }
            });
        } else {
            return source;
        }
    }
}
