/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.logging;

import static org.jboss.as.logging.LogFileResourceDefinition.LOG_FILE;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.PlaceholderResource.PlaceholderResourceEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.ResourceFilter;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingResource implements Resource {
    private static final List<String> FILE_RESOURCE_NAMES = Arrays.asList(
            FileHandlerResourceDefinition.FILE_HANDLER,
            PeriodicHandlerResourceDefinition.PERIODIC_ROTATING_FILE_HANDLER,
            PeriodicSizeRotatingHandlerResourceDefinition.PERIODIC_SIZE_ROTATING_FILE_HANDLER,
            SizeRotatingHandlerResourceDefinition.SIZE_ROTATING_FILE_HANDLER
    );

    private final PathManager pathManager;
    private final Resource delegate;

    public LoggingResource(final PathManager pathManager) {
        this(Resource.Factory.create(), pathManager);
    }

    public LoggingResource(final Resource delegate, final PathManager pathManager) {
        assert pathManager != null : "PathManager cannot be null";
        this.delegate = delegate;
        this.pathManager = pathManager;
    }

    @Override
    public ModelNode getModel() {
        return delegate.getModel();
    }

    @Override
    public void writeModel(final ModelNode newModel) {
        delegate.writeModel(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return delegate.isModelDefined();
    }

    @Override
    public boolean hasChild(final PathElement element) {
        if (LOG_FILE.equals(element.getKey())) {
            return hasReadableFile(element.getValue());
        }
        return delegate.hasChild(element);
    }

    @Override
    public Resource getChild(final PathElement element) {
        if (LOG_FILE.equals(element.getKey())) {
            if (hasReadableFile(element.getValue())) {
                return PlaceholderResource.INSTANCE;
            }
            return null;
        }
        return delegate.getChild(element);
    }

    @Override
    public Resource requireChild(final PathElement element) {
        if (LOG_FILE.equals(element.getKey())) {
            if (hasReadableFile(element.getValue())) {
                return PlaceholderResource.INSTANCE;
            }
            throw new NoSuchResourceException(element);
        }
        return delegate.requireChild(element);
    }

    @Override
    public boolean hasChildren(final String childType) {
        if (LOG_FILE.equals(childType)) {
            return !getChildrenNames(LOG_FILE).isEmpty();
        }
        return delegate.hasChildren(childType);
    }

    @Override
    public Resource navigate(final PathAddress address) {
        if (address.size() > 0 && LOG_FILE.equals(address.getElement(0).getKey())) {
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            return PlaceholderResource.INSTANCE;
        }
        return delegate.navigate(address);
    }

    @Override
    public Set<String> getChildTypes() {
        final Set<String> result = new LinkedHashSet<String>(delegate.getChildTypes());
        result.add(LOG_FILE);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(final String childType) {
        if (LOG_FILE.equals(childType)) {
            final Set<String> result = new LinkedHashSet<String>();
            for (File file : findFiles(pathManager.getPathEntry(ServerEnvironment.SERVER_LOG_DIR).resolvePath(),
                    Tools.readModel(delegate, -1, FileHandlerResourceFilter.INSTANCE))) {
                result.add(file.getName());
            }
            return result;
        }
        return delegate.getChildrenNames(childType);
    }

    @Override
    public Set<ResourceEntry> getChildren(final String childType) {
        if (LOG_FILE.equals(childType)) {
            final Set<String> names = getChildrenNames(childType);
            final Set<ResourceEntry> result = new LinkedHashSet<ResourceEntry>(names.size());
            for (String name : names) {
                result.add(new PlaceholderResourceEntry(LOG_FILE, name));
            }
            return result;
        }
        return delegate.getChildren(childType);
    }

    @Override
    public void registerChild(final PathElement address, final Resource resource) {
        final String type = address.getKey();
        if (LOG_FILE.equals(type)) {
            throw LoggingMessages.MESSAGES.cannotRegisterResourceOfType(type);
        }
        delegate.registerChild(address, resource);
    }

    @Override
    public Resource removeChild(final PathElement address) {
        final String type = address.getKey();
        if (LOG_FILE.equals(type)) {
            throw LoggingMessages.MESSAGES.cannotRemoveResourceOfType(type);
        }
        return delegate.removeChild(address);
    }

    @Override
    public boolean isRuntime() {
        return delegate.isRuntime();
    }

    @Override
    public boolean isProxy() {
        return delegate.isProxy();
    }

    @Override
    public Resource clone() {
        return new LoggingResource(delegate.clone(), pathManager);
    }

    private boolean hasReadableFile(final String fileName) {
        return getChildrenNames(LOG_FILE).contains(fileName);
    }

    /**
     * Finds all the files in the {@code jboss.server.log.dir} that are defined on a known file handler.
     *
     * @param model the model to read
     *
     * @return a list of files or an empty list if no files were found
     */
    static List<File> findFiles(final String logDir, final ModelNode model) {
        if (logDir == null) {
            return Collections.emptyList();
        }
        final File[] logFiles = new File(logDir).listFiles(AllowedFilesFilter.create(model));
        final List<File> result;
        if (logFiles == null) {
            result = Collections.emptyList();
        } else {
            result = Arrays.asList(logFiles);
            Collections.sort(result);
        }
        return result;
    }

    private static class FileHandlerResourceFilter implements ResourceFilter {

        static final FileHandlerResourceFilter INSTANCE = new FileHandlerResourceFilter();

        @Override
        public boolean accepts(final PathAddress address, final Resource resource) {
            final PathElement last = address.getLastElement();
            return last == null || FILE_RESOURCE_NAMES.contains(last.getKey());
        }
    }

    private static class AllowedFilesFilter implements FileFilter {

        private final List<String> allowedNames;

        private AllowedFilesFilter(final List<String> allowedNames) {
            this.allowedNames = allowedNames;
        }

        static AllowedFilesFilter create(final ModelNode subsystemModel) {
            final List<String> allowedNames = new ArrayList<String>();
            findFileNames(subsystemModel, allowedNames);
            return new AllowedFilesFilter(allowedNames);
        }

        private static void findFileNames(final ModelNode model, final List<String> names) {
            // Get all the file names from the model
            for (Property resource : model.asPropertyList()) {
                final String name = resource.getName();
                if (FILE_RESOURCE_NAMES.contains(name)) {
                    for (Property handlerResource : resource.getValue().asPropertyList()) {
                        final ModelNode handlerModel = handlerResource.getValue();
                        // This should always exist, but better to be safe
                        if (handlerModel.hasDefined(CommonAttributes.FILE.getName())) {
                            final ModelNode fileModel = handlerModel.get(CommonAttributes.FILE.getName());
                            if (fileModel.hasDefined(PathResourceDefinition.PATH.getName())) {
                                names.add(fileModel.get(PathResourceDefinition.PATH.getName()).asString());
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean accept(final File pathname) {
            if (pathname.canRead()) {
                final String name = pathname.getName();
                // Let's do a best guess on the file
                for (String allowedName : allowedNames) {
                    if (name.equals(allowedName) || name.startsWith(allowedName)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
