/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate.management;

import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jipijapa.management.spi.Statistics;

/**
 * Contains management support for Hibernate
 *
 * @author Scott Marlow
 */
public class HibernateManagementAdaptor implements ManagementAdaptor {

    // shared (per classloader) instance for all Hibernate 4.3 Jakarta Persistence deployments
    private static final HibernateManagementAdaptor INSTANCE = new HibernateManagementAdaptor();

    private final Statistics statistics = new HibernateStatistics();

    private static final String PROVIDER_LABEL = "hibernate-persistence-unit";
    private static final String VERSION = "Hibernate ORM 4.3.x";

    private HibernateManagementAdaptor() {

    }

    /**
     * The management statistics are shared across all Hibernate 4 Jakarta Persistence deployments
     * @return shared instance for all Hibernate 4 Jakarta Persistence deployments
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
