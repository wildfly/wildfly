/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.parser;

import java.util.Set;

import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

public class TestDA1 implements DeploymentAspect {

    @Override
    public void setLast(boolean isLast) {

    }

    @Override
    public boolean isLast() {
        return false;
    }

    @Override
    public String getProvides() {
        return null;
    }

    @Override
    public void setProvides(String provides) {

    }

    @Override
    public String getRequires() {
        return null;
    }

    @Override
    public void setRequires(String requires) {

    }

    @Override
    public void setRelativeOrder(int relativeOrder) {

    }

    @Override
    public int getRelativeOrder() {
        return 0;
    }

    @Override
    public void start(Deployment dep) {

    }

    @Override
    public void stop(Deployment dep) {

    }

    @Override
    public Set<String> getProvidesAsSet() {
        return null;
    }

    @Override
    public Set<String> getRequiresAsSet() {
        return null;
    }

    @Override
    public ClassLoader getLoader() {
        return null;
    }

}
