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

package org.jipijapa.plugin.spi;

import javax.persistence.EntityManagerFactory;

/**
 * EntityManagerFactoryBuilder is based on org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder.
 * Represents a 2-phase JPA bootstrap process for building an EntityManagerFactory.
 *
 * @author Scott Marlow
 * @author Steve Ebersole
 */
public interface EntityManagerFactoryBuilder {
    /**
     * Build {@link EntityManagerFactory} instance
     *
     * @return The built {@link EntityManagerFactory}
     */
    EntityManagerFactory build();

    /**
     * Cancel the building processing. This is used to signal the builder to release any resources in the case of
     * something having gone wrong during the bootstrap process
     */
    void cancel();

    /**
     * Allows passing in a Java EE ValidatorFactory (delayed from constructing the builder, AKA phase 2) to be used
     * in building the EntityManagerFactory
     *
     * @param validatorFactory The ValidatorFactory
     *
     * @return {@code this}, for method chaining
     */
    EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory);

}
