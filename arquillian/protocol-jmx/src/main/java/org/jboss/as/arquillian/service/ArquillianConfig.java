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
package org.jboss.as.arquillian.service;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * [TODO]
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ArquillianConfig {

    static final AttachmentKey<ArquillianConfig> KEY = AttachmentKey.create(ArquillianConfig.class);

    private DeploymentUnit deploymentUnit;
    private List<String> testClasses = new ArrayList<String>();

    ArquillianConfig(DeploymentUnit deploymentUnit){
        this.deploymentUnit = deploymentUnit;
    }

    DeploymentUnit getDeploymentUnit() {
        return deploymentUnit;
    }


    List<String> getTestClasses(){
        return testClasses;
    }

    void addTestClass(String testClass) {
        testClasses.add(testClass);
    }
}
