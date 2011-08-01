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

package org.jboss.as.mc.descriptor;

import org.jboss.as.mc.BeanState;
import org.jboss.msc.service.ServiceName;

/**
 * Injected value.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class InjectedValueConfig extends ValueConfig {
    private String dependency;
    private BeanState state;

    @Override
    public void visit(ConfigVisitor visitor) {
        BeanState required = state;
        if (required == null)
            required = BeanState.INSTALLED;

        ServiceName name = BeanMetaDataConfig.JBOSS_MC_POJO.append(dependency).append(required.name());
        visitor.addDependency(name, getValue());
    }

    public void setDependency(String dependency) {
        this.dependency = dependency;
    }

    public void setState(BeanState state) {
        this.state = state;
    }
}
