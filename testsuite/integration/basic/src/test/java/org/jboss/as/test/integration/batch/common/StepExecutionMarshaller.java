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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.Metric.MetricType;
import javax.batch.runtime.StepExecution;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class StepExecutionMarshaller {

    static final String ID = "id";
    static final String NAME = "name";
    static final String STATUS = "status";
    static final String START_TIME = "startTime";
    static final String END_TIME = "endTime";
    static final String EXIT_STATUS = "exitStatus";
    static final String PERSISTENT_USER_DATA = "persistentUserData";
    static final String METRICS = "METRICS";
    private static final String METRIC = "metric";
    private static final String METRIC_TYPE = "type";
    private static final String METRIC_VALUE = "value";

    public static String marshall(final StepExecution stepExecution) throws IOException {
        final StringWriter writer = new StringWriter();
        final JsonGenerator generator = Json.createGenerator(writer);
        generator.writeStartObject()
                .write(ID, stepExecution.getStepExecutionId())
                .write(NAME, stepExecution.getStepName())
                .write(STATUS, stepExecution.getBatchStatus().toString())
                .write(START_TIME, stepExecution.getStartTime().getTime())
                .write(END_TIME, stepExecution.getEndTime().getTime())
                .write(EXIT_STATUS, stepExecution.getExitStatus())
                .write(PERSISTENT_USER_DATA, serialize(stepExecution.getPersistentUserData()));
        generator.writeStartObject(METRICS);
        for (Metric metric : stepExecution.getMetrics()) {
            generator.writeStartObject(METRIC);
            generator.write(METRIC_TYPE, metric.getType().toString());
            generator.write(METRIC_VALUE, metric.getValue());
            generator.writeEnd();
        }
        generator.writeEnd();

        // End main object
        generator.writeEnd();
        generator.close();
        return writer.toString();
    }

    public static StepExecution unmarshall(final String json) throws IOException, ClassNotFoundException {
        final JsonParser parser = Json.createParser(new StringReader(json));
        final StepExecutionBuilder builder = StepExecutionBuilder.create();

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
                        case END_TIME:
                            if (value != null) {
                                builder.setEndTime(Long.parseLong(value));
                            }
                            break;
                        case START_TIME:
                            if (value != null) {
                                builder.setStartTime(Long.parseLong(value));
                            }
                            break;
                        case PERSISTENT_USER_DATA:
                            builder.setPersistentUserData(deserialize(value));
                        case METRICS:
                            String k = null;
                            String metricType = null;
                            String metricValue = null;
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
                                        if (k == null) {
                                            throw new IllegalStateException(String.format("No key for value '%s'. Parsing position: %s%n\t%s", value, parser.getLocation(), json));
                                        }
                                        switch (k) {
                                            case METRIC_TYPE:
                                                metricType = parser.getString();
                                                break;
                                            case METRIC_VALUE:
                                                metricValue = parser.getString();
                                                break;
                                        }
                                        if (metricType != null && metricValue != null) {
                                            final MetricType type = MetricType.valueOf(metricType);
                                            final long v = Long.parseLong(parser.getString());
                                            final Metric m = new Metric() {
                                                @Override
                                                public MetricType getType() {
                                                    return type;
                                                }

                                                @Override
                                                public long getValue() {
                                                    return v;
                                                }
                                            };
                                            builder.addMetric(m);
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

    private static String serialize(final Serializable serializable) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(serializable);
        out.flush();
        return baos.toString();
    }

    private static Serializable deserialize(final String data) throws IOException, ClassNotFoundException {
        if (data == null) {
            return null;
        }
        final ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes());
        final ObjectInputStream in = new ObjectInputStream(bais);
        return (Serializable) in.readObject();
    }

    static class StepExecutionBuilder {
        private long id;
        private String name;
        private BatchStatus status;
        private long startTime;
        private long endTime;
        private String exitStatus;
        private Serializable persistentUserData;
        private final Collection<Metric> metrics;

        private StepExecutionBuilder() {
            id = -1L;
            name = null;
            status = null;
            startTime = 0L;
            endTime = 0L;
            exitStatus = null;
            persistentUserData = null;
            metrics = new ArrayList<Metric>();
        }

        public static StepExecutionBuilder create() {
            return new StepExecutionBuilder();
        }

        public StepExecutionBuilder setId(final long id) {
            this.id = id;
            return this;
        }

        public StepExecutionBuilder setName(final String name) {
            this.name = name;
            return this;
        }

        public StepExecutionBuilder setStatus(final BatchStatus status) {
            this.status = status;
            return this;
        }

        public StepExecutionBuilder setStartTime(final long startTime) {
            this.startTime = startTime;
            return this;
        }

        public StepExecutionBuilder setEndTime(final long endTime) {
            this.endTime = endTime;
            return this;
        }

        public StepExecutionBuilder setExitStatus(final String exitStatus) {
            this.exitStatus = exitStatus;
            return this;
        }

        public StepExecutionBuilder setPersistentUserData(final Serializable persistentUserData) {
            this.persistentUserData = persistentUserData;
            return this;
        }

        public StepExecutionBuilder addMetric(final Metric metric) {
            metrics.add(metric);
            return this;
        }

        public StepExecution build() {
            final long id = this.id;
            final String name = this.name;
            final BatchStatus status = this.status;
            final long startTime = this.startTime;
            final long endTime = this.endTime;
            final String exitStatus = this.exitStatus;
            final Serializable persistentUserData = this.persistentUserData;
            final Metric[] metrics = this.metrics.toArray(new Metric[this.metrics.size()]);
            return new StepExecution() {
                @Override
                public long getStepExecutionId() {
                    return id;
                }

                @Override
                public String getStepName() {
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
                public Serializable getPersistentUserData() {
                    return persistentUserData;
                }

                @Override
                public Metric[] getMetrics() {
                    return Arrays.copyOf(metrics, metrics.length);
                }
            };
        }
    }
}
