package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.connector.StatisticsDescriptionProvider;
import org.jboss.as.connector.pool.PoolMetrics;
import org.jboss.as.connector.subsystems.ClearStatisticsHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;

/**
*
 * Listener that registers data source statistics with the management model
 *
*/
public class DataSourceStatisticsListener extends AbstractServiceListener<Object> {

    private final ManagementResourceRegistration registration;
    private final String dsName;

    public DataSourceStatisticsListener(final ManagementResourceRegistration registration, final String dsName) {
        this.registration = registration;
        this.dsName = dsName;
    }

    public void transition(final ServiceController<? extends Object> controller,
                           final ServiceController.Transition transition) {

        switch (transition) {
            case STARTING_to_UP: {

                CommonDeployment deploymentMD = ((AbstractDataSourceService) controller.getService()).getDeploymentMD();

                StatisticsPlugin jdbcStats = deploymentMD.getDataSources()[0].getStatistics();
                StatisticsPlugin poolStats = deploymentMD.getDataSources()[0].getPool().getStatistics();
                int jdbcStatsSize = jdbcStats.getNames().size();
                int poolStatsSize = poolStats.getNames().size();
                if (jdbcStatsSize > 0 || poolStatsSize > 0) {
                    ManagementResourceRegistration subRegistration = registration.registerOverrideModel(dsName, DataSourcesSubsystemProviders.OVERRIDE_DS_DESC);

                    if (jdbcStatsSize > 0) {
                        ManagementResourceRegistration jdbcRegistration = subRegistration.registerSubModel(PathElement.pathElement("statistics", "jdbc"), new StatisticsDescriptionProvider(DataSourcesSubsystemProviders.RESOURCE_NAME, "statistics", jdbcStats));
                        jdbcRegistration.setRuntimeOnly(true);
                        jdbcRegistration.registerOperationHandler("clear-statistics", new ClearStatisticsHandler(jdbcStats), DataSourcesSubsystemProviders.CLEAR_STATISTICS_DESC, false);

                        for (String statName : jdbcStats.getNames()) {
                            jdbcRegistration.registerMetric(statName, new PoolMetrics.ParametrizedPoolMetricsHandler(jdbcStats));
                        }

                    }

                    if (poolStatsSize > 0) {
                        ManagementResourceRegistration poolRegistration = subRegistration.registerSubModel(PathElement.pathElement("statistics", "pool"), new StatisticsDescriptionProvider(DataSourcesSubsystemProviders.RESOURCE_NAME, "statistics", poolStats));
                        poolRegistration.setRuntimeOnly(true);
                        poolRegistration.registerOperationHandler("clear-statistics", new ClearStatisticsHandler(poolStats), DataSourcesSubsystemProviders.CLEAR_STATISTICS_DESC, false);

                        for (String statName : poolStats.getNames()) {
                            poolRegistration.registerMetric(statName, new PoolMetrics.ParametrizedPoolMetricsHandler(poolStats));
                        }
                    }
                }
                break;


            }
            case UP_to_STOP_REQUESTED: {

                ManagementResourceRegistration subRegistration = registration.getOverrideModel(dsName);
                if (subRegistration != null) {
                    subRegistration.unregisterSubModel(PathElement.pathElement("statistics", "jdbc"));
                    subRegistration.unregisterSubModel(PathElement.pathElement("statistics", "pool"));
                    registration.unregisterOverrideModel(dsName);
                }
                break;

            }
        }
    }
}
