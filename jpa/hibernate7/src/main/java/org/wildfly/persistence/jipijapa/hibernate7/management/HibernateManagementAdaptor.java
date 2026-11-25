/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7.management;

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
