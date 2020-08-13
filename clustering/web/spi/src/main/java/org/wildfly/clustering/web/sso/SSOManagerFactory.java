/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.sso;

import org.wildfly.clustering.ee.Batch;

/**
 * Factory for creating SSO manager instances.
 * @param <A> authentication type
 * @param <D> deployment type
 * @param <S> session type
 * @param <B> batch type
 */
public interface SSOManagerFactory<A, D, S, B extends Batch> {
    /**
     * Creates a new SSO manager using the specified configuration.
     * @param <C> marshalling context type
     * @param <L> local context type
     * @param config a SSO manager configuration
     * @return a new SSO manager
     */
    <C, L> SSOManager<A, D, S, L, B> createSSOManager(SSOManagerConfiguration<C, L> config);
}
