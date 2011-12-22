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

package org.jboss.as.test.integration.ejb.entity.cmp.relationship;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.TransactionWrappingSessionBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author John Bailey
 */
public abstract class AbstractRelationshipTest extends AbstractCmpTest{
    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-relationship.jar");
        jar.addPackage(org.jboss.as.test.integration.ejb.entity.cmp.relationship.manyToManyBidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.test.integration.ejb.entity.cmp.relationship.manyToManyUnidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.test.integration.ejb.entity.cmp.relationship.manyToOneUnidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToManyBidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToManyUnidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneUnidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectional.ABTestCase.class.getPackage());
        jar.addPackage(AbstractRelationshipTest.class.getPackage());
        jar.addAsManifestResource("ejb/entity/cmp/relationship/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/entity/cmp/relationship/jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }
}
