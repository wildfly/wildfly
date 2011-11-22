/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jaxr.scout;

import javax.xml.registry.infomodel.Association;
import javax.xml.registry.infomodel.Concept;
import javax.xml.registry.infomodel.Organization;

/** A Utility class that is used by the JAXR Testsuite
 *  @author <mailto:Anil.Saldhana@jboss.org>Anil Saldhana
 *  @since  Mar 9, 2005
 */
public class ScoutUtil
{
   public static void validateAssociation(Association a, String sourceOrgName)
           throws Exception
   {
      Organization o = (Organization)a.getSourceObject();
      if(o.getName() == null || o.getName().getValue() == null)
        throw new Exception("Source OrgName in association is null");
      if (!o.getName().getValue().equals(sourceOrgName))
      {
         throw new Exception("Invalid Source Org in Association");
      }
      o = (Organization)a.getTargetObject();
      if(o.getName()== null || o.getName().getValue() == null)
         throw new Exception("Target OrgName in association is null");;
      Concept atype = a.getAssociationType();
      if(atype.getName() == null || atype.getName().getValue() ==null)
       throw new Exception("Concept stored in Association" );
   }
}
