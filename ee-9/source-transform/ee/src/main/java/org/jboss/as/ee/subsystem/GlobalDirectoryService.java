/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.ee.subsystem;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.PathEntry;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ee.subsystem.GlobalDirectoryResourceDefinition.GlobalDirectory;
import org.jboss.modules.PathUtils;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible creating a {@code GlobalDirectory} which contains the information of a configured global directory.
 *
 * @author Yeray Borges
 */
public class GlobalDirectoryService implements Service {
    private final Supplier<PathManager> pathManagerSupplier;
    private final String path;
    private final String relativeTo;
    private final PathResolver pathResolver;
    private final Consumer<GlobalDirectory> consumer;
    private final String name;

    public GlobalDirectoryService(Supplier<PathManager> pathManagerSupplier, Consumer<GlobalDirectory> consumer, String name, String path, String relativeTo) {
        this.pathManagerSupplier = checkNotNullParam("pathManagerSupplier", pathManagerSupplier);
        this.path = checkNotNullParam("path", path);
        this.name = checkNotNullParam("name", name);
        this.relativeTo = relativeTo;
        this.pathResolver = new PathResolver();
        this.consumer = consumer;
    }

    @Override
    public void start(StartContext context) throws StartException {
        pathResolver.path(path);
        pathResolver.relativeTo(relativeTo, pathManagerSupplier.get());
        GlobalDirectory globaldirectory = new GlobalDirectory(pathResolver.resolve(), name);
        consumer.accept(globaldirectory);
    }

    @Override
    public void stop(StopContext context) {
        pathResolver.clear();
    }

    private static class PathResolver {

        private String path;
        private String relativeTo;
        private PathManager pathManager;

        private PathManager.Callback.Handle callbackHandle;

        PathResolver path(String path) {
            this.path = path;

            return this;
        }

        PathResolver relativeTo(String relativeTo, PathManager pathManager) {
            this.relativeTo = relativeTo;
            this.pathManager = pathManager;
            return this;
        }

        Path resolve() {
            String relativeToPath = AbsolutePathService.isAbsoluteUnixOrWindowsPath(path) ? null : relativeTo;
            Path resolvedPath = Paths.get(PathUtils.canonicalize(pathManager.resolveRelativePathEntry(path, relativeToPath))).normalize();
            if (relativeTo != null) {
                callbackHandle = pathManager.registerCallback(relativeTo, new org.jboss.as.controller.services.path.PathManager.Callback() {

                    @Override
                    public void pathModelEvent(PathManager.PathEventContext eventContext, String name) {
                        if (eventContext.isResourceServiceRestartAllowed() == false) {
                            eventContext.reloadRequired();
                        }
                    }

                    @Override
                    public void pathEvent(PathManager.Event event, PathEntry pathEntry) {
                        // Service dependencies should trigger a stop and start.
                    }
                }, PathManager.Event.REMOVED, PathManager.Event.UPDATED);
            }
            return resolvedPath;
        }

        void clear() {
            if (callbackHandle != null) {
                callbackHandle.remove();
                callbackHandle = null;
            }
        }
    }
}
