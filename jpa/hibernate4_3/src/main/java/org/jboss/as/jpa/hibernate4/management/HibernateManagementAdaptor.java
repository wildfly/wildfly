/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate4.management;

import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jipijapa.management.spi.Statistics;

/**
 * Contains management support for Hibernate
 *
 * @author Scott Marlow
 */
public class HibernateManagementAdaptor implements ManagementAdaptor {

    // shared (per classloader) instance for all Hibernate 4.3 JPA deployments
    private static final HibernateManagementAdaptor INSTANCE = new HibernateManagementAdaptor();

    private final Statistics statistics = new HibernateStatistics();

    private static final String PROVIDER_LABEL = "hibernate-persistence-unit";
    private static final String VERSION = "Hibernate ORM 4.3.x";

    /**
     * The management statistics are shared across all Hibernate 4 JPA deployments
     * @return shared instance for all Hibernate 4 JPA deployments
     */
    public static HibernateManagementAdaptor getInstance() {
        return INSTANCE;
    }

    @Override
    public String getIdentificationLabel() {
        return PROVIDER_LABEL;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Statistics getStatistics() {
        return statistics;
    }


}
