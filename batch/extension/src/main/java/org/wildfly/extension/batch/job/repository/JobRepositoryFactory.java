/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.job.repository;

import java.util.Properties;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;
import org.jboss.as.ee.component.EEModuleDescription;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JobRepositoryFactory {

    /**
     * The key for the JNDI name. Used with JDBC job repositories
     */
    public static final String JNDI_NAME = "datasource-jndi";

    private static final JobRepositoryFactory INSTANCE = new JobRepositoryFactory();

    private volatile JobRepositoryType type;
    private volatile String jndiName;

    private JobRepositoryFactory() {
    }

    public static JobRepositoryFactory getInstance() {
        return INSTANCE;
    }

    public JobRepository getJobRepository(final EEModuleDescription moduleDescription) {
        final JobRepositoryType type = this.type;
        if (JobRepositoryType.JDBC == type) {
            String jndiName = this.jndiName;
            if (jndiName == null) {
                jndiName = moduleDescription.getDefaultResourceJndiNames().getDataSource();
            }
            final Properties configProperties = new Properties();
            configProperties.setProperty(JNDI_NAME, jndiName);
            return JdbcRepository.create(configProperties);
        }
        return InMemoryRepository.getInstance();
    }

    public boolean requiresJndiName() {
        return JobRepositoryType.JDBC == type && jndiName == null;
    }

    public void setJndiName(final String jndiName) {
        this.jndiName = jndiName;
    }

    public void setJobRepositoryType(final String jobRepositoryType) {
        setJobRepositoryType(JobRepositoryType.of(jobRepositoryType));
    }

    public void setJobRepositoryType(final JobRepositoryType type) {
        this.type = type;
    }

}
