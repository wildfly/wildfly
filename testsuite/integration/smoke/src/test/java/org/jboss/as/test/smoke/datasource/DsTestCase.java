/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.connector.subsystems.datasources.WildFlyDataSource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertTrue(rs.next());
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
            Assertions.assertTrue(rs.next());
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
