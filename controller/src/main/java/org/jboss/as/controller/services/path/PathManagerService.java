/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.services.path;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathEntry.PathResolver;
import org.jboss.as.controller.services.path.PathManager.Callback.Handle;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class PathManagerService implements PathManager, Service<PathManager> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("path", "manager");

    //@GuardedBy(pathEntries)
    private final Map<String, PathEntry> pathEntries = new HashMap<String, PathEntry>();

    //@GuardedBy(pathEntries)
    private final Map<String, Set<String>> dependenctRelativePaths = new HashMap<String, Set<String>>();

    //@GuardedBy(callbacks)
    private final Map<String, Map<Event, Set<Callback>>> callbacks = new HashMap<String, Map<Event, Set<Callback>>>();

    protected PathManagerService() {
    }

    public final void addPathManagerResources(Resource resource) {
        synchronized (pathEntries) {
            for (PathEntry pathEntry : pathEntries.values()) {
                resource.registerChild(PathElement.pathElement(PATH, pathEntry.getName()), new HardcodedPathResource(PATH, pathEntry));
            }
        }
    }

    public final String resolveRelativePathEntry(String path, String relativeTo) {
        if (relativeTo == null) {
            return AbsolutePathService.convertPath(path);
        } else {
            PathEntry pathEntry;
            synchronized (pathEntries) {
                pathEntry = pathEntries.get(relativeTo);
                if (pathEntry == null) {
                    throw MESSAGES.pathEntryNotFound(path);
                }
                return RelativePathService.doResolve(pathEntry.resolvePath(), path);
            }
        }
    }

    @Override
    public final Handle registerCallback(String name, Callback callback, Event...events) {
        synchronized (callbacks) {
            Map<Event, Set<Callback>> callbacksByEvent = callbacks.get(name);
            if (callbacksByEvent == null) {
                callbacksByEvent = new HashMap<PathManager.Event, Set<Callback>>();
                callbacks.put(name, callbacksByEvent);
            }
            for (Event event : events) {
                Set<Callback> callbackSet = callbacksByEvent.get(event);
                if (callbackSet == null) {
                    callbackSet = new HashSet<PathManager.Callback>();
                    callbacksByEvent.put(event, callbackSet);
                }
                callbackSet.add(callback);
            }
        }
        return new HandleImpl(name, callback, events);
    }

    @Override
    public final void start(StartContext context) throws StartException {
    }

    @Override
    public final void stop(StopContext context) {
    }

    @Override
    public final PathManagerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    protected final ServiceController<?> addHardcodedAbsolutePath(final ServiceTarget serviceTarget, final String pathName, final String path) {
        ServiceController<?>  controller = addAbsolutePathService(serviceTarget, pathName, path);
        addPathEntry(pathName, path, null, true);
        return controller;
    }

    public final PathEntry getPathEntry(String pathName) {
        synchronized (pathEntries) {
            PathEntry pathEntry = pathEntries.get(pathName);
            if (pathEntry == null) {
                throw MESSAGES.pathEntryNotFound(pathName);
            }
            return pathEntry;
        }
    }

    final void changePathServices(final OperationContext operationContext, String pathName, String path) {
        PathEntry pathEntry = findPathEntry(pathName);

        removePathService(operationContext, pathName);
        if (pathEntry.getRelativeTo() == null) {
            addAbsolutePathService(operationContext.getServiceTarget(), pathEntry.getName(), path);
        } else {
            addRelativePathService(operationContext.getServiceTarget(), pathEntry.getName(), path, false, pathEntry.getRelativeTo());
        }
    }

    final void changeRelativePathServices(final OperationContext operationContext, String pathName, String relativeTo) {
        PathEntry pathEntry = findPathEntry(pathName);

        removePathService(operationContext, pathEntry.getName());
        if (relativeTo == null) {
            addAbsolutePathService(operationContext.getServiceTarget(), pathEntry.getName(), pathEntry.getPath());
        } else {
            addRelativePathService(operationContext.getServiceTarget(), pathEntry.getName(), pathEntry.getPath(), false, relativeTo);
        }
    }

    final List<PathEntry> getPaths() {
        synchronized (pathEntries) {
            return new ArrayList<PathEntry>(pathEntries.values());
        }
    }

    final void removePathService(final OperationContext operationContext, final String pathName) {
        final ServiceController<?> serviceController = operationContext.getServiceRegistry(true).getService(AbstractPathService.pathNameOf(pathName));
        if (serviceController != null) {
            operationContext.removeService(serviceController);
        }
    }

    final void removePathEntry(final String pathName, boolean check) throws OperationFailedException{
        synchronized (pathEntries) {
            PathEntry pathEntry = pathEntries.get(pathName);
            if (pathEntry.isReadOnly()) {
                throw MESSAGES.pathEntryIsReadOnly(pathName);
            }

            Set<String> dependents = dependenctRelativePaths.get(pathName);
            if (dependents != null) {
                throw MESSAGES.cannotRemovePathWithDependencies(pathName, dependents);
            }
            pathEntries.remove(pathName);
            triggerCallbacksForEvent(pathEntry, Event.REMOVED);
            if (pathEntry.getRelativeTo() != null) {
                dependents = dependenctRelativePaths.get(pathEntry.getRelativeTo());
                if (dependents != null) {
                    dependents.remove(pathEntry.getName());
                    if (dependents.size() == 0) {
                        dependenctRelativePaths.remove(pathEntry.getRelativeTo());
                    }
                }
            }
        }
    }

    final ServiceController<?> addAbsolutePathService(final ServiceTarget serviceTarget, final String pathName, final String path, final ServiceListener<?>... listeners) {
        return AbsolutePathService.addService(pathName, path, serviceTarget, null, listeners);
    }

    final ServiceController<?> addRelativePathService(final ServiceTarget serviceTarget, final String pathName, final String path,
            final boolean possiblyAbsolute, final String relativeTo, final ServiceListener<?>... listeners) {
        if (possiblyAbsolute && AbstractPathService.isAbsoluteUnixOrWindowsPath(path)) {
            return addAbsolutePathService(serviceTarget, pathName, path, listeners);
        } else {
            return RelativePathService.addService(AbstractPathService.pathNameOf(pathName), path, possiblyAbsolute, relativeTo, serviceTarget, null, listeners);
        }
    }

    final PathEntry addPathEntry(final String pathName, final String path, final String relativeTo, final boolean readOnly) {
        PathEntry pathEntry;
        synchronized (pathEntries) {
            if (pathEntries.containsKey(pathName)) {
                throw MESSAGES.pathEntryAlreadyExists(pathName);
            }
            pathEntry = new PathEntry(pathName, path, relativeTo, readOnly, relativeTo == null ? absoluteResolver : relativeResolver);
            pathEntries.put(pathName, pathEntry);

            if (relativeTo != null) {
                addDependent(pathName, relativeTo);
            }
        }
        triggerCallbacksForEvent(pathEntry, Event.ADDED);
        return pathEntry;
    }

    final void changeRelativePath(String pathName, String relativePath, boolean check) throws OperationFailedException {
        PathEntry pathEntry = findPathEntry(pathName);
        synchronized (pathEntries) {
            if (pathEntry.getRelativeTo() != null) {
                Set<String> dependents = dependenctRelativePaths.get(pathEntry.getRelativeTo());
                dependents.remove(pathEntry.getName());
            }
            if (check && relativePath != null && pathEntries.get(relativePath) == null) {
                throw MESSAGES.pathEntryNotFound(pathName);
            }
            pathEntry.setRelativeTo(relativePath);
            pathEntry.setPathResolver(relativePath == null ? absoluteResolver : relativeResolver);
            addDependent(pathEntry.getName(), pathEntry.getRelativeTo());
        }
        triggerCallbacksForEvent(pathEntry, Event.UPDATED);
    }

    final void changePath(String pathName, String path) {
        PathEntry pathEntry = findPathEntry(pathName);
        pathEntry.setPath(path);
        triggerCallbacksForEvent(pathEntry, Event.UPDATED);
    }

    //Must be called with pathEntries lock taken
    private void addDependent(String pathName, String relativeTo) {
        if (relativeTo != null) {
            Set<String> dependents = dependenctRelativePaths.get(relativeTo);
            if (dependents == null) {
                dependents = new HashSet<String>();
                dependenctRelativePaths.put(relativeTo, dependents);
            }
            dependents.add(pathName);
        }
    }


    private PathEntry findPathEntry(String pathName) {
        PathEntry pathEntry;
        synchronized (pathEntries) {
            pathEntry = pathEntries.get(pathName);
            if (pathEntry == null) {
                throw MESSAGES.pathEntryNotFound(pathName);
            }
        }
        return pathEntry;
    }

    private void triggerCallbacksForEvent(PathEntry pathEntry, Event event) {
        Set<PathEntry> allEntries = null;
        synchronized (pathEntries) {
            if (event == Event.UPDATED) {
                allEntries = new LinkedHashSet<PathEntry>();
                allEntries.add(pathEntry);
                getAllDependents(allEntries, pathEntry.getName());
            } else {
                allEntries = Collections.singleton(pathEntry);
            }
        }

        Map<PathEntry, Set<Callback>> triggerCallbacks = new LinkedHashMap<PathEntry, Set<Callback>>();
        synchronized (callbacks) {
            for (PathEntry pe : allEntries) {
                Map<Event, Set<Callback>> callbacksByEvent = callbacks.get(pe.getName());
                if (callbacksByEvent != null) {
                    Set<Callback> callbacksForEntry = callbacksByEvent.get(event);
                    if (callbacksForEntry != null) {
                        triggerCallbacks.put(pe, new LinkedHashSet<Callback>(callbacksForEntry));
                    }
                }
            }
        }

        for (Map.Entry<PathEntry, Set<Callback>> entry : triggerCallbacks.entrySet()) {
            for (Callback cb : entry.getValue()) {
                cb.pathEvent(event, entry.getKey());
            }
        }
    }

    PathEventContextImpl checkRestartRequired(OperationContext operationContext, String name, Event event) {
        Set<String> allEntries = null;
        synchronized (pathEntries) {
            if (event == Event.UPDATED) {
                allEntries = new LinkedHashSet<String>();
                allEntries.add(name);
                getAllDependentsForRestartCheck(allEntries, name);
            } else {
                allEntries = Collections.singleton(name);
            }
        }

        Map<String, Set<Callback>> triggerCallbacks = new LinkedHashMap<String, Set<Callback>>();
        synchronized (callbacks) {
            for (String pathName : allEntries) {
                Map<Event, Set<Callback>> callbacksByEvent = callbacks.get(pathName);
                if (callbacksByEvent != null) {
                    Set<Callback> callbacksForEntry = callbacksByEvent.get(event);
                    if (callbacksForEntry != null) {
                        triggerCallbacks.put(pathName, new LinkedHashSet<Callback>(callbacksForEntry));
                    }
                }
            }
        }

        PathEventContextImpl pathEventContext = new PathEventContextImpl(operationContext, event);
        for (Map.Entry<String, Set<Callback>> entry : triggerCallbacks.entrySet()) {
            for (Callback cb : entry.getValue()) {
                cb.pathModelEvent(pathEventContext, entry.getKey());
                if (pathEventContext.restart) {
                    return pathEventContext;
                }
            }
        }
        return pathEventContext;
    }

    //Call with pathEntries lock taken
    private void getAllDependents(Set<PathEntry> result, String name) {
        Set<String> depNames = dependenctRelativePaths.get(name);
        if (depNames == null) {
            return;
        }
        for (String dep : depNames) {
            PathEntry entry = pathEntries.get(dep);
            if (entry != null) {
                result.add(entry);
                getAllDependents(result, dep);
            }
        }
    }

    //Call with pathEntries lock taken
    private void getAllDependentsForRestartCheck(Set<String> result, String name) {
        Set<String> depNames = dependenctRelativePaths.get(name);
        if (depNames == null) {
            return;
        }
        for (String dep : depNames) {
            PathEntry entry = pathEntries.get(dep);
            if (entry != null) {
                result.add(dep);
                getAllDependentsForRestartCheck(result, dep);
            }
        }
    }


    private final PathResolver absoluteResolver = new PathResolver() {
        @Override
        public String resolvePath(String name, String path, String relativeTo, PathResolver resolver) {
            return AbsolutePathService.convertPath(path);
        }

        @Override
        public boolean isResolved(String relativeTo) {
            return true;
        }
    };

    private final PathResolver relativeResolver = new PathResolver() {

        @Override
        public String resolvePath(String name,  String path, String relativeTo, PathResolver resolver) {
            PathEntry relativeEntry;
            synchronized (pathEntries) {
                relativeEntry = pathEntries.get(relativeTo);
                if (relativeEntry == null) {
                    throw new IllegalStateException("Could not find relativeTo path '" + relativeTo + "' for relative path '" + name);
                }
            }
            return RelativePathService.doResolve(relativeEntry.resolvePath(), path);
        }

        @Override
        public boolean isResolved(String relativeTo) {
            synchronized (pathEntries) {
                return pathEntries.containsKey(relativeTo);
            }

        }
    };

    private class HandleImpl implements Handle {
        private final String pathName;
        private final Callback callback;
        private final Event[] events;

        public HandleImpl(String pathName, Callback callback, Event...events) {
            this.pathName = pathName;
            this.callback = callback;
            this.events = events;
        }

        public void remove() {
            synchronized (callbacks) {
                Map<Event, Set<Callback>> callbacksByEvent = callbacks.get(pathName);
                if (callbacksByEvent != null) {
                    for (Event event : events) {
                        Set<Callback> callbackSet = callbacksByEvent.get(event);
                        if (callbackSet != null) {
                            callbackSet.remove(callback);
                        }
                        if (callbackSet.isEmpty()) {
                            callbacksByEvent.remove(event);
                        }
                    }
                    if (callbacksByEvent.isEmpty()) {
                        callbacks.remove(pathName);
                    }
                }
            }
        }
    }

    static class PathEventContextImpl implements PathEventContext {
        private final OperationContext operationContext;
        private final Event event;
        private volatile boolean reload;
        private volatile boolean restart;

        PathEventContextImpl(OperationContext operationContext, Event event) {
            this.operationContext = operationContext;
            this.event = event;
        }

        public boolean isBooting() {
            return operationContext.isBooting();
        }

        public boolean isNormalServer() {
            return operationContext.isNormalServer();
        }

        public boolean isResourceServiceRestartAllowed() {
            return operationContext.isResourceServiceRestartAllowed();
        }

        public void reloadRequired() {
            reload = true;
            operationContext.reloadRequired();
        }

        public void restartRequired() {
            restart = true;
            operationContext.restartRequired();
        }

        @Override
        public Event getEvent() {
            return event;
        }

        void revert() {
            if (restart) {
                operationContext.revertRestartRequired();
            }
            if (reload) {
                operationContext.revertReloadRequired();
            }
        }

        boolean isInstallServices() {
            return !restart && !reload;
        }
    }
}
