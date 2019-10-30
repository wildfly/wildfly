/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate5;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

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
