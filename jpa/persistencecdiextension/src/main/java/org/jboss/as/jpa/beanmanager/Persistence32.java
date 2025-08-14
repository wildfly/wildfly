/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import java.lang.annotation.Annotation;
import java.util.List;

import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.persistence.SchemaManager;
import org.jipijapa.plugin.spi.IntegrationWithCDIBag;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.SchemaManagerBeanCreator;

/**
 * Persistence32
 *
 * @author Scott Marlow
 */
public class Persistence32 implements SchemaManagerBeanCreator {

    @Override
    public void schemaManager(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitMetadata persistenceUnitMetadata, List<String> qualifiers, IntegrationWithCDIBag integrationWithCDIBag) throws ClassNotFoundException {
        String scope = IntegratePersistenceAfterBeanDiscovery.dependentScoped;

        BeanConfigurator<SchemaManager> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(SchemaManager.class);


        Class<? extends Annotation> scopeAnnotation = persistenceUnitMetadata.getClassLoader().loadClass(scope).asSubclass(Annotation.class);
        beanConfigurator.scope(scopeAnnotation);

        for (String qualifier : qualifiers) {
            final Class<? extends Annotation> qualifierType = persistenceUnitMetadata.getClassLoader()
                    .loadClass(qualifier)
                    .asSubclass(Annotation.class);
            // beanConfigurator.addQualifier(qualifierType);
            beanConfigurator.addQualifier(IntegratePersistenceAfterBeanDiscovery.ScopeProxy.createProxy(qualifierType));
        }
        Class<?> schemaManagerClass = SchemaManager.class;
        beanConfigurator.beanClass(schemaManagerClass);
        beanConfigurator.produceWith(c -> {
                    return integrationWithCDIBag.getEntityManagerFactory().getSchemaManager();
                }
        );


    }
}
