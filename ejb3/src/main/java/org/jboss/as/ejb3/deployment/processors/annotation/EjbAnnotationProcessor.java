/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.Asynchronous;
import javax.ejb.BeforeCompletion;
import javax.ejb.Startup;

import org.jboss.as.ee.component.deployers.BooleanAnnotationInformationFactory;
import org.jboss.as.ee.metadata.AbstractEEAnnotationProcessor;
import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;

/**
 * Processes EJB annotations and attaches them to the {@link org.jboss.as.ee.component.EEModuleClassDescription}
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
        factories.add(new LegacySecurityDomainAnnotationInformationFactory());
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
