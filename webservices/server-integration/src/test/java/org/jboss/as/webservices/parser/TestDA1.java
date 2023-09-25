/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
