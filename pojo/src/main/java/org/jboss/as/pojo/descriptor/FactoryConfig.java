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

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.service.BeanInfo;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

/**
 * Factory value.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class FactoryConfig extends ValueConfig {
    private static final long serialVersionUID = 1L;

    private String bean;
    private BeanState state;

    protected final transient InjectedValue<BeanInfo> beanInfo = new InjectedValue<BeanInfo>();
    protected final transient InjectedValue<Object> value = new InjectedValue<Object>();

    protected Object getClassValue(Class<?> type) {
        return value.getValue();
    }

    @Override
    public void visit(ConfigVisitor visitor) {
        if (bean != null) {
            visitor.addDependency(bean, BeanState.DESCRIBED, beanInfo);
            ServiceName name = BeanMetaDataConfig.toBeanName(bean, state);
            visitor.addDependency(name, value); // direct name, since we have describe already
        }
        super.visit(visitor);
    }

    public Class<?> getType(ConfigVisitor visitor, ConfigVisitorNode previous) {
        throw PojoLogger.ROOT_LOGGER.tooDynamicFromFactory();
    }

    public void setBean(String dependency) {
        this.bean = dependency;
    }

    public void setState(BeanState state) {
        this.state = state;
    }

    public BeanInfo getBeanInfo() {
        return beanInfo.getValue();
    }
}
