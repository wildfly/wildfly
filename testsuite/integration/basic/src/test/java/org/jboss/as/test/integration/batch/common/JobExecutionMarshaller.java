/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.batch.common;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JobExecutionMarshaller {

    static final String ID = "id";
    static final String NAME = "name";
    static final String STATUS = "status";
    static final String EXIT_STATUS = "exitStatus";
    static final String CREATE_TIME = "createTime";
    static final String END_TIME = "endTime";
    static final String LAST_UPDATE_TIME = "lastUpdateTime";
    static final String START_TIME = "startTime";
    static final String PROPERTIES = "properties";

    public static String marshall(final JobExecution jobExecution) {
        final StringWriter writer = new StringWriter();
        final JsonGenerator generator = Json.createGenerator(writer);
        generator.writeStartObject()
                .write(ID, jobExecution.getExecutionId())
                .write(NAME, jobExecution.getJobName())
                .write(STATUS, jobExecution.getBatchStatus().toString())
                .write(EXIT_STATUS, jobExecution.getExitStatus())
                .write(CREATE_TIME, jobExecution.getCreateTime().getTime())
                .write(END_TIME, jobExecution.getEndTime().getTime())
                .write(LAST_UPDATE_TIME, jobExecution.getLastUpdatedTime().getTime())
                .write(START_TIME, jobExecution.getStartTime().getTime());
        // Write out properties
        generator.writeStartObject(PROPERTIES);
        final Properties params = jobExecution.getJobParameters();
        for (String key : params.stringPropertyNames()) {
            generator.write(key, params.getProperty(key));
        }
        generator.writeEnd();

        // End main object
        generator.writeEnd();
        generator.close();
        return writer.toString();
    }

    public static JobExecution unmarshall(final String json) {
        final JsonParser parser = Json.createParser(new StringReader(json));
        final JobExecutionBuilder builder = JobExecutionBuilder.create();

        String key = null;
        while (parser.hasNext()) {
            final Event event = parser.next();
            switch (event) {
                case KEY_NAME:
                    key = parser.getString();
                    break;
                case VALUE_FALSE:
                case VALUE_NULL:
                case VALUE_NUMBER:
                case VALUE_STRING:
                case VALUE_TRUE:
                    final String value = parser.getString();
                    if (key == null) {
                        throw new IllegalStateException(String.format("No key for value '%s'. Parsing position: %s%n\t%s", value, parser.getLocation(), json));
                    }
                    switch (key) {
                        case ID:
                            if (value != null) {
                                builder.setId(Long.parseLong(value));
                            }
                            break;
                        case NAME:
                            builder.setName(value);
                            break;
                        case STATUS:
                            if (value != null) {
                                builder.setStatus(BatchStatus.valueOf(value));
                            }
                            break;
                        case EXIT_STATUS:
                            builder.setExitStatus(value);
                            break;
                        case CREATE_TIME:
                            if (value != null) {
                                builder.setCreateTime(Long.parseLong(value));
                            }
                            break;
                        case END_TIME:
                            if (value != null) {
                                builder.setEndTime(Long.parseLong(value));
                            }
                            break;
                        case LAST_UPDATE_TIME:
                            if (value != null) {
                                builder.setLastUpdatedTime(Long.parseLong(value));
                            }
                            break;
                        case START_TIME:
                            if (value != null) {
                                builder.setStartTime(Long.parseLong(value));
                            }
                            break;
                        case PROPERTIES:
                            String k = null;
                            while (parser.hasNext()) {
                                final Event e = parser.next();
                                switch (e) {
                                    case KEY_NAME:
                                        k = parser.getString();
                                        break;
                                    case VALUE_FALSE:
                                    case VALUE_NULL:
                                    case VALUE_NUMBER:
                                    case VALUE_STRING:
                                    case VALUE_TRUE:
                                        if (k != null) {
                                            builder.addParameter(k, parser.getString());
                                        }
                                        break;
                                }
                            }
                            break;
                    }
                    break;
            }
        }
        parser.close();
        return builder.build();
    }

    static class JobExecutionBuilder {
        private long id;
        private String name;
        private BatchStatus status;
        private long startTime;
        private long endTime;
        private String exitStatus;
        private long createTime;
        private long lastUpdatedTime;
        private final Properties params;

        private JobExecutionBuilder() {
            id = -1L;
            name = null;
            status = null;
            startTime = 0L;
            endTime = 0L;
            exitStatus = null;
            createTime = 0L;
            lastUpdatedTime = 0L;
            params = new Properties();
        }

        public static JobExecutionBuilder create() {
            return new JobExecutionBuilder();
        }

        public JobExecutionBuilder setId(final long id) {
            this.id = id;
            return this;
        }

        public JobExecutionBuilder setName(final String name) {
            this.name = name;
            return this;
        }

        public JobExecutionBuilder setStatus(final BatchStatus status) {
            this.status = status;
            return this;
        }

        public JobExecutionBuilder setStartTime(final long startTime) {
            this.startTime = startTime;
            return this;
        }

        public JobExecutionBuilder setEndTime(final long endTime) {
            this.endTime = endTime;
            return this;
        }

        public JobExecutionBuilder setExitStatus(final String exitStatus) {
            this.exitStatus = exitStatus;
            return this;
        }

        public JobExecutionBuilder setCreateTime(final long createTime) {
            this.createTime = createTime;
            return this;
        }

        public JobExecutionBuilder setLastUpdatedTime(final long lastUpdatedTime) {
            this.lastUpdatedTime = lastUpdatedTime;
            return this;
        }

        public JobExecutionBuilder addParameter(final String key, final String value) {
            params.setProperty(key, value);
            return this;
        }

        public JobExecution build() {
            final long id = this.id;
            final String name = this.name;
            final BatchStatus status = this.status;
            final long startTime = this.startTime;
            final long endTime = this.endTime;
            final String exitStatus = this.exitStatus;
            final long createTime = this.createTime;
            final long lastUpdatedTime = this.lastUpdatedTime;
            final Properties params = new Properties();
            params.putAll(this.params);
            return new JobExecution() {

                @Override
                public long getExecutionId() {
                    return id;
                }

                @Override
                public String getJobName() {
                    return name;
                }

                @Override
                public BatchStatus getBatchStatus() {
                    return status;
                }

                @Override
                public Date getStartTime() {
                    return new Date(startTime);
                }

                @Override
                public Date getEndTime() {
                    return new Date(endTime);
                }

                @Override
                public String getExitStatus() {
                    return exitStatus;
                }

                @Override
                public Date getCreateTime() {
                    return new Date(createTime);
                }

                @Override
                public Date getLastUpdatedTime() {
                    return new Date(lastUpdatedTime);
                }

                @Override
                public Properties getJobParameters() {
                    return params;
                }

                @Override
                public String toString() {
                    final StringBuilder sb = new StringBuilder("JobExecutionBuilder{");
                    sb.append("id=").append(id);
                    sb.append(", name='").append(name).append('\'');
                    sb.append(", status=").append(status);
                    sb.append(", startTime=").append(startTime);
                    sb.append(", endTime=").append(endTime);
                    sb.append(", exitStatus='").append(exitStatus).append('\'');
                    sb.append(", createTime=").append(createTime);
                    sb.append(", lastUpdatedTime=").append(lastUpdatedTime);
                    sb.append(", params=").append(params);
                    sb.append('}');
                    return sb.toString();
                }
            };
        }
    }
}
