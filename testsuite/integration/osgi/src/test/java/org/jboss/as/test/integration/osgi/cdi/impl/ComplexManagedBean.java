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
package org.jboss.as.test.integration.osgi.cdi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.inject.Produces;

import org.jboss.as.test.integration.osgi.api.PaymentProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;


/**
 * @author Thomas.Diesler@jboss.com
 * @since 09-Jul-2012
 */
@ManagedBean
public class ComplexManagedBean {

    private BundleContext context;

    @Resource
    public void setContext(BundleContext context) {
        this.context = context;
    }

    private List<PaymentProvider> providers = new ArrayList<PaymentProvider>();

    @PostConstruct
    public void start() {
        ServiceTracker tracker = new ServiceTracker(context, PaymentProvider.class.getName(), null) {

            @Override
            public Object addingService(ServiceReference reference) {
                PaymentProvider service = (PaymentProvider) super.addingService(reference);
                providers.add(service);
                return service;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                providers.remove(service);
                super.removedService(reference, service);
            }
        };
        tracker.open();
    }

    @Produces
    public List<String> getPaymentProviders() {
        List<String> result = new ArrayList<String>();
        for (PaymentProvider prov : providers) {
            result.add(prov.getName());
        }
        Collections.sort(result);
        return result;
    }
}
