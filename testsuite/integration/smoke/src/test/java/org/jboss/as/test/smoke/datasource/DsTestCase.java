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
package org.jboss.as.test.smoke.datasource;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.connector.subsystems.datasources.WildFlyDataSource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
public class DsTestCase {
    private static final String JNDI_NAME = "java:jboss/datasources/ExampleDS";

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "ds-example.jar");
        archive.addClass(DsTestCase.class);
        archive.addClass(WildFlyDataSource.class);
        return archive;
    }

    @Test
    public void testDatasource() throws Exception {
        InitialContext context = new InitialContext();
        DataSource ds = (DataSource) context.lookup(JNDI_NAME);
        Connection conn = ds.getConnection();
        ResultSet rs = conn.prepareStatement("select 1").executeQuery();
        Assert.assertTrue(rs.next());
        conn.close();
    }

    @Test
    public void testDatasourceSerialization() throws Exception {
        InitialContext context = new InitialContext();
        DataSource originalDs = (DataSource) context.lookup(JNDI_NAME);
        //serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        DataSource ds;
        ObjectInput in = null;

        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(originalDs);
            byte[] bytes = bos.toByteArray();


            //deserialize
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            try {
                in = new ObjectInputStream(bis);
                ds = (DataSource) in.readObject();
            } finally {
                try {
                    bis.close();
                } catch (IOException ex) {
                    // ignore close exception
                }
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    // ignore close exception
                }
            }
            //use
            Connection conn = ds.getConnection();
            ResultSet rs = conn.prepareStatement("select 1").executeQuery();
            Assert.assertTrue(rs.next());
            conn.close();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }

        }


    }
}
