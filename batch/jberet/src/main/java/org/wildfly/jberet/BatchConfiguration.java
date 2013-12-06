package org.wildfly.jberet;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.wildfly.jberet._private.WildFlyBatchMessages;

/**
 * Configures properties to the batch configuration.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchConfiguration {

    /**
     * Valid job repository types
     */
    public enum JobRepositoryType {
        JDBC {
            @Override
            public String toString() {
                return "jdbc";
            }
        },
        IN_MEMORY {
            @Override
            public String toString() {
                return "in-memory";
            }
        };

        private static final Map<String, JobRepositoryType> MAP;

        static {
            MAP = new HashMap<>();
            MAP.put(JDBC.toString(), JDBC);
            MAP.put(IN_MEMORY.toString(), IN_MEMORY);
        }

        public static JobRepositoryType of(final String value) {
            if (MAP.containsKey(value)) {
                return MAP.get(value);
            }
            throw WildFlyBatchMessages.MESSAGES.invalidJobRepositoryType(value);
        }
    }

    /**
     * The key for the job repository type
     */
    public static final String JOB_REPOSITORY_TYPE = "job-repository-type";

    /**
     * The key for the JNDI name. Used with JDBC job repositories
     */
    public static final String JNDI_NAME = "datasource-jndi";

    private static final BatchConfiguration INSTANCE = new BatchConfiguration();

    private final ConcurrentMap<String, String> properties = new ConcurrentHashMap<>();

    private BatchConfiguration() {
    }

    /**
     * Gets the instance of the configuration.
     *
     * @return the instance of the configuration
     */
    public static BatchConfiguration getInstance() {
        return INSTANCE;
    }

    /**
     * Creates {@link Properties properties} for the configuration.
     *
     * @return a new properties configuration containing the values added to this configuration
     */
    public Properties createProperties() {
        final Properties result = new Properties();
        result.putAll(properties);
        return result;
    }

    /**
     * Checks to see if a JNDI name is required. A JNDI name is said to be required if the repository type is {@link
     * JobRepositoryType#JDBC JDBC} and a JNDI name was not previously {@link #setJndiName(String) set}.
     *
     * @return {@code true} if a JNDI name is require, otherwise {@code false}
     */
    public boolean requiresJndiName() {
        return JobRepositoryType.JDBC.toString().equals(properties.get(JOB_REPOSITORY_TYPE)) && !properties.containsKey(JNDI_NAME);
    }

    /**
     * Puts a key value pair into the configuration.
     *
     * @param key   the key
     * @param value the value
     *
     * @return the previous value associated with the key or {@code null} if there wasn't one
     */
    public String put(final String key, final String value) {
        return properties.put(key, value);
    }

    /**
     * Sets the job repository type.
     * <p/>
     * Short cut for
     * <pre>
     *     <code>BatchConfiguration.getInstance().put(BatchConfiguration.JOB_REPOSITORY_TYPE, value);</code>
     * </pre>
     *
     * @param jobRepositoryType the job repository type
     */
    public void setJobRepositoryType(final String jobRepositoryType) {
        if (JobRepositoryType.MAP.containsKey(jobRepositoryType)) {
            put(JOB_REPOSITORY_TYPE, jobRepositoryType);
        } else {
            throw WildFlyBatchMessages.MESSAGES.invalidJobRepositoryType(jobRepositoryType);
        }
    }


    /**
     * Sets the JNDI name.
     * <p/>
     * Short cut for
     * <pre>
     *     <code>BatchConfiguration.getInstance().put(BatchConfiguration.JNDI_NAME, value);</code>
     * </pre>
     *
     * @param jndiName the JNDI name
     */
    public void setJndiName(final String jndiName) {
        put(JNDI_NAME, jndiName);
    }
}
