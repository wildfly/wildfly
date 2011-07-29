/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.io.Serializable;

/**
 * Ctor meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConstructorConfig implements Serializable, ConfigVisitorNode {
    private static final long serialVersionUID = 1L;

    private String factoryClass;
    private String factoryMethod;
    private ValueConfig factory;
    private ValueConfig[] parameters;

    @Override
    public void visit(ConfigVisitor visitor) {
        if (factory != null)
            factory.visit(visitor);
        if (parameters != null)
            for (ValueConfig param : parameters)
                param.visit(visitor);
    }

    public String getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(String factoryClass) {
        this.factoryClass = factoryClass;
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }

    public void setFactoryMethod(String factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public ValueConfig getFactory() {
        return factory;
    }

    public void setFactory(ValueConfig factory) {
        this.factory = factory;
    }

    public ValueConfig[] getParameters() {
        return parameters;
    }

    public void setParameters(ValueConfig[] parameters) {
        this.parameters = parameters;
    }
}