/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.datasource.remove;

import org.jboss.as.test.shared.TimeoutUtil;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@Stateless
@Remote(RemoveDsBeanRemote.class)
public class RemoveDsBean implements RemoveDsBeanRemote {

    @Resource(mappedName = "java:jboss/datasources/RemoveDS")
    private DataSource removeDS;

    public void testDatasource() {
        assert removeDS != null;
        try (Connection connection = removeDS.getConnection()) {
            assert connection != null;
            assert connection.isValid(TimeoutUtil.adjust(1000));
        } catch (SQLException e) {
            throw new RuntimeException("Connection is not valid.", e);
        }
    }
}
