/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.smoke.embedded.deployment.ds;

import static junit.framework.Assert.*;

import java.sql.ResultSet;
import java.sql.Connection;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Ignore;

/**
 * Tests an ability to deploy *-ds.xml datasource definition JBQA-5872
 *
 * @author Vladimir Rastseluev
 */

@RunWith(Arquillian.class)

public class DsDeploymentTestCase {
	private String user1="SA";


    @Deployment
    public static Archive<?> deploy() throws Exception{
    	
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,"test.ear");
        
    	JavaArchive jt=ShrinkWrap.create(JavaArchive.class,"test.jar")      
        		.addClasses(DsDeploymentTestCase.class);
         ear.addAsLibrary(jt)
         .addAsManifestResource("ds/h2-ds.xml", "h2-ds.xml")
         .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,org.jboss.ironjacamar.jdbcadapters\n"),"MANIFEST.MF");
        return ear;
    }
    

    @ArquillianResource
    private InitialContext ctx;


    @Test 
    public void testDataSourceDefinition() throws Exception {
    	
    	DataSource bean=(DataSource)ctx.lookup("java:jboss/datasources/DDs");
    	Connection conn=null;ResultSet result=null;
    	
    	try{
        	conn=bean.getConnection();
         	result=conn.createStatement().executeQuery("select current_user()");
    	}
        catch (Exception sqle) {
            sqle.printStackTrace();
        }
    	finally{
    			try{
					assertNotNull("result is undefined",result);
					assertTrue("result is undefined",result.next());
					assertTrue("user="+result.getString(1)+", but must be some of '"+user1+"' instances",result.getString(1).indexOf(user1)>=0);
  					
    			}
    			catch(Exception e){e.printStackTrace();}
    			finally{
    				try{
    					if(result!=null) result.close();
    					if(conn!=null) conn.close();
    					
    				}
    				catch(Exception e){e.printStackTrace();}
    		}
    	}
    	
    }
}
