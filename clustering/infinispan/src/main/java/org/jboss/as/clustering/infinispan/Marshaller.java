/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.marshall.VersionAwareMarshaller;
import org.infinispan.marshall.jboss.ExternalizerTable;
import org.infinispan.marshall.jboss.JBossMarshaller;

/**
 * Workaround for https://issues.jboss.org/browse/ISPN-3698
 * @author Paul Ferraro
 */
public class Marshaller extends VersionAwareMarshaller {

    @Override
    public void inject(Cache cache, Configuration cfg, InvocationContextContainer icc, ExternalizerTable extTable, GlobalConfiguration globalCfg) {
        PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    Field field = VersionAwareMarshaller.class.getDeclaredField("defaultMarshaller");
                    field.setAccessible(true);
                    try {
                        field.set(Marshaller.this, new JBossMarshaller());
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    } finally {
                        field.setAccessible(false);
                    }
                    return null;
                } catch (NoSuchFieldException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        AccessController.doPrivileged(action);
        super.inject(cache, cfg, icc, extTable, globalCfg);
    }
}
