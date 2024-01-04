/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.AfterBegin;
import jakarta.ejb.AfterCompletion;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.BeforeCompletion;
import jakarta.ejb.Startup;

import org.jboss.as.ee.component.deployers.BooleanAnnotationInformationFactory;
import org.jboss.as.ee.metadata.AbstractEEAnnotationProcessor;
import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;

/**
 * Processes Jakarta Enterprise Beans annotations and attaches them to the {@link org.jboss.as.ee.component.EEModuleClassDescription}
 *
 * @author Stuart Douglas
 */
public class EjbAnnotationProcessor extends AbstractEEAnnotationProcessor {

    final List<ClassAnnotationInformationFactory> factories;

    public EjbAnnotationProcessor() {
        final List<ClassAnnotationInformationFactory> factories = new ArrayList<ClassAnnotationInformationFactory>();
        factories.add(new LockAnnotationInformationFactory());
        factories.add(new ConcurrencyManagementAnnotationInformationFactory());
        factories.add(new AccessTimeoutAnnotationInformationFactory());
        factories.add(new TransactionAttributeAnnotationInformationFactory());
        factories.add(new TransactionTimeoutAnnotationInformationFactory());
        factories.add(new TransactionManagementAnnotationInformationFactory());
        factories.add(new RemoveAnnotationInformationFactory());
        factories.add(new BooleanAnnotationInformationFactory<Startup>(Startup.class));
        factories.add(new StatefulTimeoutAnnotationInformationFactory());
        factories.add(new BooleanAnnotationInformationFactory<Asynchronous>(Asynchronous.class));
        factories.add(new DependsOnAnnotationInformationFactory());

        factories.add(new ResourceAdaptorAnnotationInformationFactory());
        factories.add(new DeliveryActiveAnnotationInformationFactory());
        factories.add(new DeliveryGroupAnnotationInformationFactory());
        factories.add(new InitAnnotationInformationFactory());

        // pool
        factories.add(new PoolAnnotationInformationFactory());

        //session synchronization
        factories.add(new BooleanAnnotationInformationFactory<AfterBegin>(AfterBegin.class));
        factories.add(new BooleanAnnotationInformationFactory<BeforeCompletion>(BeforeCompletion.class));
        factories.add(new BooleanAnnotationInformationFactory<AfterCompletion>(AfterCompletion.class));

        //security annotations
        factories.add(new RunAsAnnotationInformationFactory());
        factories.add(new RunAsPrincipalAnnotationInformationFactory());
        factories.add(new SecurityDomainAnnotationInformationFactory());
        factories.add(new DeclareRolesAnnotationInformationFactory());
        factories.add(new RolesAllowedAnnotationInformationFactory());
        factories.add(new BooleanAnnotationInformationFactory<DenyAll>(DenyAll.class));
        factories.add(new BooleanAnnotationInformationFactory<PermitAll>(PermitAll.class));

        //view annotations
        factories.add(new LocalHomeAnnotationInformationFactory());
        factories.add(new RemoteHomeAnnotationInformationFactory());

        // clustering/cache annotations
        factories.add(new ClusteredAnnotationInformationFactory());
        factories.add(new CacheAnnotationInformationFactory());
        factories.add(new ClusteredSingletonAnnotationInformationFactory());

        this.factories = Collections.unmodifiableList(factories);
    }


    @Override
    protected List<ClassAnnotationInformationFactory> annotationInformationFactories() {
        return factories;
    }
}
