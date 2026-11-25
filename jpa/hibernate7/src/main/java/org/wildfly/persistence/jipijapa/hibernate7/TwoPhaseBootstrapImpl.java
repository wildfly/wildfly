/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7;

import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.jipijapa.plugin.spi.EntityManagerFactoryBuilder;

/**
 * TwoPhaseBootstrapImpl
 *
 * @author Scott Marlow
 */
public class TwoPhaseBootstrapImpl implements EntityManagerFactoryBuilder {

    final org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder entityManagerFactoryBuilder;

    public TwoPhaseBootstrapImpl(final PersistenceUnitInfo info, final Map map) {
        entityManagerFactoryBuilder =
                    Bootstrap.getEntityManagerFactoryBuilder(info, map);
    }

    @Override
    public EntityManagerFactory build() {
        return entityManagerFactoryBuilder.build();
    }

    @Override
    public void cancel() {
        entityManagerFactoryBuilder.cancel();
    }

    @Override
    public EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory) {
        entityManagerFactoryBuilder.withValidatorFactory(validatorFactory);
        return this;
    }

}
