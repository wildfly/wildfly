/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.jpa.processor;

import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;

import javax.persistence.spi.PersistenceProvider;

/**
 * @author Stuart Douglas
 */
public class JpaAttachments {

    public static final AttachmentKey<String> ADAPTOR_CLASS_NAME = AttachmentKey.create(String.class);

    /**
     * List<PersistenceUnitMetadataImpl> that represents the JPA persistent units
     */
    public static final AttachmentKey<PersistenceProviderDeploymentHolder> DEPLOYED_PERSISTENCE_PROVIDER = AttachmentKey.create(PersistenceProviderDeploymentHolder.class);

    /**
     * List of app provided providers.
     */
    public static final AttachmentKey<AttachmentList<PersistenceProvider>> APP_PROVIDERS = AttachmentKey.createList(PersistenceProvider.class);

    /**
     * List ignored PU, that don't get turned into PUServiceImpl.
     */
    public static final AttachmentKey<AttachmentList<String>> IGNORED_PU_SERVICES = AttachmentKey.createList(String.class);

    private JpaAttachments() {

    }
}
