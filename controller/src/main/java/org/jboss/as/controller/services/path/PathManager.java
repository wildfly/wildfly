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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.services.path.PathManager.Callback.Handle;


/**
 * The client interface for the {@link PathManagerService}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface PathManager {

    /**
     * Resolves a relative path
     *
     * @param path an absolute path if {@code relativeTo} is {@code null}, the relative path to {@code relativeTo} otherwise
     * @param relativeTo the name of the path this is relative to, may be {@code null}
     * @return the resolved path
     * @throws IllegalStateException if there is no path registered under {@code relativeTo}
     */
    String resolveRelativePathEntry(String path, String relativeTo);

    /**
     * Gets a path entry
     *
     * @param name the name of the path
     * @return the path
     * @throws IllegalStateException if there is no path registered under {@code path}
     */
    PathEntry getPathEntry(String name);

    /**
     * Registers a callback for when a path is added/changed/removed
     *
     * @param name the name of the path
     * @param callback the callback instance that will be called when one of the events occur
     * @param events the events we are interested in
     * @return a handle to unregister the callback
     */
    Handle registerCallback(String name, Callback callback, Event...events);

    /**
     * A callback, see {@link PathManager#registerCallback(String, Callback, Event...)}
     */
    interface Callback {

        /**
         * Called when a path is modified in the model. This happens before any changes are made to the path
         * in the path manager. If {@link PathEventContext#reloadRequired()} or {@link PathEventContext#restartRequired()}
         * are called the path will not get updated in the path manager, and {@code pathEvent) does not get
         * called.
         *
         * @param eventContext the event
         * @param the name of the path being modified
         */
        void pathModelEvent(PathEventContext eventContext, String name);

        /**
         * Called once the model has been successfully updated, and the path has been updated in the path manager.
         *
         * @param event the event
         * @param pathEntry the path entry after the event takes place
         */
        void pathEvent(Event event, PathEntry pathEntry);

        /**
         * A handle to a callback
         */
        interface Handle {
            /**
             * Removes the callback
             */
            void remove();
        }
    }

    /**
     * An event triggered when changes are made to a path entry
     */
    enum Event {
        /** A path entry was added */
        ADDED,
        /** A path entry was removed */
        REMOVED,
        /** A path entry was updated */
        UPDATED
    }

    interface PathEventContext {
        /**
         * @see OperationContext#isBooting()
         */
        boolean isBooting();

        /**
         * @see OperationContext#isNormalServer()
         */
        boolean isNormalServer();

        /**
         * @see OperationContext#isResourceServiceRestartAllowed()
         */
        boolean isResourceServiceRestartAllowed();

        /**
         * @see OperationContext#reloadRequired()
         */
        void reloadRequired();

        /**
         * @see OperationContext#restartRequired()
         */
        void restartRequired();

        /**
         * Get the event triggered
         *
         * @return the event
         */
        Event getEvent();
    }

    static class ReloadServerCallback {
        public static Callback create() {
            return  new Callback() {
                @Override
                public void pathModelEvent(PathEventContext eventContext, String name) {
                    eventContext.reloadRequired();
                }

                @Override
                public void pathEvent(Event event, PathEntry pathEntry) {
                }
            };
        }
    }
}
