/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * Injection source for remote Jakarta Enterprise Beans lookups. If the target type is known
 * then the bean will be narrowed to that type
 *
 * @author Stuart Douglas
 */
public class EjbLookupInjectionSource extends InjectionSource {

    private final String lookup;
    private final String targetTypeName;
    private final Class<?> targetType;

    public EjbLookupInjectionSource(final String lookup, final String targetType) {
        this.lookup = lookup;
        this.targetTypeName = targetType;
        this.targetType = null;
    }

    public EjbLookupInjectionSource(final String lookup, final Class<?> targetType) {
        this.lookup = lookup;
        this.targetType = targetType;
        this.targetTypeName = null;
    }


    @Override
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final Class<?> type;
        if (targetType != null) {
            type = targetType;
        } else if (targetTypeName != null) {
            try {
                type = ClassLoadingUtils.loadClass(targetTypeName, phaseContext.getDeploymentUnit());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            type = null;
        }

        injector.inject(new ManagedReferenceFactory() {
            @Override
            public ManagedReference getReference() {
                try {
                    final Object value = new InitialContext().lookup(lookup);

                    return new ManagedReference() {
                        @Override
                        public void release() {

                        }

                        @Override
                        public Object getInstance() {
                            if(type != null && value instanceof org.omg.CORBA.Object) {
                                return PortableRemoteObject.narrow(value, type);
                            } else {
                                return value;
                            }
                        }
                    };
                } catch (NamingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final EjbLookupInjectionSource that = (EjbLookupInjectionSource) o;

        if (lookup != null ? !lookup.equals(that.lookup) : that.lookup != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return lookup != null ? lookup.hashCode() : 0;
    }
}
