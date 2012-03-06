/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Handler;

import org.jboss.as.logging.CommonAttributes;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.Logger.AttachmentKey;
import org.jboss.logmanager.filters.DenyAllFilter;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class Handlers {

    public static final Logger ROOT = Logger.getLogger(CommonAttributes.ROOT_LOGGER_NAME);

    public static final AttachmentKey<Map<String, Filter>> DISABLED_HANDLERS_KEY = new AttachmentKey<Map<String, Filter>>();

    private static final Object HANDLER_LOCK = new Object();

    /**
     * Enables the handler if it was previously disabled.
     * <p/>
     * If it was not previously disable, nothing happens.
     *
     * @param handler     the handler to disable.
     * @param handlerName the name of the handler to enable.
     */
    static void enableHandler(final Handler handler, final String handlerName) {
        final Map<String, Filter> disableHandlers = ROOT.getAttachment(DISABLED_HANDLERS_KEY);
        if (disableHandlers != null && disableHandlers.containsKey(handlerName)) {
            synchronized (HANDLER_LOCK) {
                final Filter filter = disableHandlers.get(handlerName);
                handler.setFilter(filter);
                disableHandlers.remove(handlerName);
            }
        }
    }

    /**
     * Disables the handler if the handler exists and is not already disabled.
     * <p/>
     * If the handler does not exist or is already disabled nothing happens.
     *
     * @param handler     the handler to disable.
     * @param handlerName the handler name to disable.
     */
    static void disableHandler(final Handler handler, final String handlerName) {
        Map<String, Filter> disableHandlers = ROOT.getAttachment(DISABLED_HANDLERS_KEY);
        if (disableHandlers == null) {
            disableHandlers = new HashMap<String, Filter>();
            final Map<String, Filter> current = ROOT.attachIfAbsent(DISABLED_HANDLERS_KEY, disableHandlers);
            if (current != null) {
                disableHandlers = current;
            }
        }
        synchronized (HANDLER_LOCK) {
            if (!disableHandlers.containsKey(handlerName)) {
                disableHandlers.put(handlerName, handler.getFilter());
                handler.setFilter(DenyAllFilter.getInstance());
            }
        }
    }
}
