/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.single.jdbcstore;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import org.infinispan.Cache;

/**
 * @author Martin Gencur
 */
@ManagedBean("infinispan")
public class TransactionalInfinispanManagedBean {

    private static final String CACHE_JNDI_NAME = "java:jboss/infinispan/cache/jdbccontainer/jdbccache";
    private static final String DATASOURCE_JNDI_NAME = "java:jboss/datasources/ExampleDS";

    @Resource(name = CACHE_JNDI_NAME)
    private Cache<Integer, Object> cache;

    @Resource
    private UserTransaction tx;

    @PostConstruct
    public void start() {
        assert cache != null;
        assert tx != null;
    }

    public void testTxPutCommit() throws Exception {
        tx.begin();
        cache.put(1, "v1");
        cache.put(2, "v2");
        assert "v1".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 0;
        tx.commit();
        assert "v1".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
    }

    public void testTxPutRollback() throws Exception {
        tx.begin();
        cache.put(1, "v1");
        cache.put(2, "v2");
        assert "v1".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 0;
        tx.rollback();
        assert cache.get(3) == null;
        assert cache.get(4) == null;
        assert rowCount() == 0; // no change in DB
    }

    public void testTxRemoveCommit() throws Exception {
        initializeCache();
        tx.begin();
        assert "v1".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
        cache.remove(1);
        assert cache.get(1) == null;
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
        tx.commit();
        assert cache.get(1) == null;
        assert "v2".equals(cache.get(2));
        assert rowCount() == 1;
    }

    public void testTxRemoveRollback() throws Exception {
        initializeCache();
        tx.begin();
        assert "v1".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
        cache.remove(1);
        assert cache.get(1) == null;
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
        tx.rollback();
        assert "v1".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2; // no change in DB
    }

    public void testTxAlterCommit() throws Exception {
        initializeCache();
        tx.begin();
        assert "v1".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
        cache.put(1, "v1_new");
        assert "v1_new".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
        tx.commit();
        assert "v1_new".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
    }

    public void testTxAlterRollback() throws Exception {
        initializeCache();
        tx.begin();
        assert "v1".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
        cache.put(1, "v1_new");
        assert "v1_new".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
        tx.rollback();
        assert "v1".equals(cache.get(1));
        assert "v2".equals(cache.get(2));
        assert rowCount() == 2;
    }

    private int rowCount() throws Exception {
        Context ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup(DATASOURCE_JNDI_NAME);
        Connection conn = ds.getConnection();
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery("SELECT id FROM stringbased_jdbccache");
        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
        }
        return rowCount;
    }
    
    private void initializeCache() throws Exception {
        try {
            tx.begin();
            cache.clear();
            cache.put(1, "v1");
            cache.put(2, "v2");
            assert cache.keySet().size() == 2;
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException(e);
        }
    }
}
