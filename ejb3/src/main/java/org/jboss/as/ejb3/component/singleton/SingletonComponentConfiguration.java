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

package org.jboss.as.ejb3.component.singleton;


import org.jboss.as.ee.component.EEModuleClassConfiguration;
import org.jboss.as.ejb3.component.session.SessionBeanComponentConfiguration;

/**
 * @author Jaikiran Pai
 */
public class SingletonComponentConfiguration extends SessionBeanComponentConfiguration {

    private boolean initOnStartup;

    /**
     * Construct a new instance.
     *
     * @param description the original component description
     */
    public SingletonComponentConfiguration(final SingletonComponentDescription description, final EEModuleClassConfiguration ejbClassConfiguration) {
        super(description, ejbClassConfiguration);

        this.initOnStartup = description.isInitOnStartup();

        // instance associating interceptor
        //TODO: interceptors

//        this.addComponentSystemInterceptorFactory(new ImmediateInterceptorFactory(new SingletonComponentInstanceAssociationInterceptor()));
        // BMT interceptor
//        if (TransactionManagementType.BEAN.equals(description.getTransactionManagementType())) {
//            addComponentSystemInterceptorFactory(new ComponentInterceptorFactory() {
//                @Override
//                protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
//                    return new SingletonBMTInterceptor((SingletonComponent) component);
//                }
//            });
//        }

        setComponentCreateServiceFactory(SingletonComponentCreateService.FACTORY);
    }

    public boolean isInitOnStartup() {
        return this.initOnStartup;
    }

}
