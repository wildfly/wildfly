/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.persistenceprovider;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of PersistenceProviderResolver
 * TODO:  look at forking/merging with Hibernate javax.persistence.spi.PersistenceProviderResolverHolder.PersistenceProviderResolverPerClassLoader
 * TODO:  add other persistence providers to the providers list
 *
 * @author Scott Marlow
 */
public class PersistenceProviderResolverImpl implements PersistenceProviderResolver {

    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        List<PersistenceProvider> providers = new ArrayList<PersistenceProvider>(1);
        try {
            Class cls = PersistenceProviderResolverImpl.class.getClassLoader().loadClass("org.hibernate.ejb" +
                ".HibernatePersistence");
            Object o = cls.newInstance();
            providers.add((PersistenceProvider) o);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        //providers.add(new HibernatePersistence());
        return providers;
    }

    @Override
    public void clearCachedProviders() {

    }
}
