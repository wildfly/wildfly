/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import java.util.function.BiConsumer;

/**
 * @author Paul Ferraro
 */
public interface SessionAttributeActivationNotifier extends AutoCloseable {
    BiConsumer<SessionAttributeActivationNotifier, Object> PRE_PASSIVATE = SessionAttributeActivationNotifier::prePassivate;
    BiConsumer<SessionAttributeActivationNotifier, Object> POST_ACTIVATE = SessionAttributeActivationNotifier::postActivate;

    /**
     * Notifies the specified attribute that it will be passivated, if interested.
     */
    void prePassivate(Object value);

    /**
     * Notifies the specified attribute that it was activated, if interested.
     */
    void postActivate(Object value);

    @Override
    void close();
}
