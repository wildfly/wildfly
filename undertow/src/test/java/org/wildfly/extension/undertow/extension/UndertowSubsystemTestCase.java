package org.wildfly.extension.undertow.extension;

import java.io.IOException;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.CookieHandler;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.undertow.LocationService;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.filters.FilterService;

/**
 * This is the barebone test example that tests subsystem
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class UndertowSubsystemTestCase extends AbstractSubsystemBaseTest {

    public UndertowSubsystemTestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("undertow-1.0.xml");
    }

    @Test
    public void testRuntime() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new AdditionalInitialization() {
        })
                .setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Assert.fail(mainServices.getBootError().toString());
        }
        ServiceController<FilterService> filter = (ServiceController<FilterService>) mainServices.getContainer().getService(UndertowService.FILTER.append("limit-connections"));
        filter.setMode(ServiceController.Mode.ACTIVE);
        FilterService filterService = filter.getService().getValue();
        HttpHandler result = filterService.createHttpHandler(new CookieHandler());
        Assert.assertNotNull("handler should have been created", result);

        final ServiceName locationServiceName = UndertowService.locationServiceName("default-server", "default-host", "/");
        ServiceController<LocationService> locationSC = (ServiceController<LocationService>) mainServices.getContainer().getService(locationServiceName);
        Assert.assertNotNull(locationSC);
        locationSC.setMode(ServiceController.Mode.ACTIVE);
        LocationService locationService = locationSC.getValue();
        Assert.assertNotNull(locationService);
        /*FilterService injectedFilter = locationService.getFilterInjector().get(0).getValue();
        Assert.assertNotNull(injectedFilter);
        */filter.setMode(ServiceController.Mode.REMOVE);

    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }
}
