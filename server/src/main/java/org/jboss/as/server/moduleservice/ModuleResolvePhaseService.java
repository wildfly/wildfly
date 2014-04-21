package org.jboss.as.server.moduleservice;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import static org.jboss.msc.service.ServiceBuilder.DependencyType.OPTIONAL;
import static org.jboss.msc.service.ServiceBuilder.DependencyType.REQUIRED;

/**
 * Module phase resolve service. Basically this service attempts to resolve
 * every dynamic transitive dependency of a module, and allows the module resolved service
 * to start once this is complete.
 *
 * @author Stuart Douglas
 */
public class ModuleResolvePhaseService implements Service<ModuleResolvePhaseService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("module", "resolve", "phase");

    private final ModuleIdentifier moduleIdentifier;

    private final Set<ModuleIdentifier> alreadyResolvedModules;

    private final int phaseNumber;

    /**
     * module specification that were resolved this phase. These are injected as the relevant spec services start.
     */
    private final Set<ModuleDefinition> moduleSpecs = Collections.synchronizedSet(new HashSet<ModuleDefinition>());

    public ModuleResolvePhaseService(final ModuleIdentifier moduleIdentifier, final Set<ModuleIdentifier> alreadyResolvedModules, final int phaseNumber) {
        this.moduleIdentifier = moduleIdentifier;
        this.alreadyResolvedModules = alreadyResolvedModules;
        this.phaseNumber = phaseNumber;
    }

    public ModuleResolvePhaseService(final ModuleIdentifier moduleIdentifier) {
        this.moduleIdentifier = moduleIdentifier;
        this.alreadyResolvedModules = Collections.emptySet();
        this.phaseNumber = 0;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        Set<ModuleDependency> nextPhaseIdentifiers = new HashSet<>();
        final Set<ModuleIdentifier> nextAlreadySeen = new HashSet<>(alreadyResolvedModules);
        for (final ModuleDefinition spec : moduleSpecs) {
            if (spec != null) { //this can happen for optional dependencies
                for (ModuleDependency dep : spec.getDependencies()) {
                    if (ServiceModuleLoader.isDynamicModule(dep.getIdentifier())) {
                        if (!alreadyResolvedModules.contains(dep.getIdentifier())) {
                            nextAlreadySeen.add(dep.getIdentifier());
                            nextPhaseIdentifiers.add(dep);
                        }
                    }
                }
            }
        }
        if (nextPhaseIdentifiers.isEmpty()) {
            ServiceModuleLoader.installModuleResolvedService(startContext.getChildTarget(), moduleIdentifier);
        } else {
            installService(startContext.getChildTarget(), moduleIdentifier, phaseNumber + 1, nextPhaseIdentifiers, nextAlreadySeen);
        }
    }

    public static void installService(final ServiceTarget serviceTarget, final ModuleDefinition moduleDefinition) {
        final ModuleResolvePhaseService nextPhaseService = new ModuleResolvePhaseService(moduleDefinition.getModuleIdentifier(), Collections.singleton(moduleDefinition.getModuleIdentifier()), 0);
        nextPhaseService.getModuleSpecs().add(moduleDefinition);
        ServiceBuilder<ModuleResolvePhaseService> builder = serviceTarget.addService(moduleSpecServiceName(moduleDefinition.getModuleIdentifier(), 0), nextPhaseService);
        builder.install();
    }

    private static void installService(final ServiceTarget serviceTarget, final ModuleIdentifier moduleIdentifier, int phaseNumber, final Set<ModuleDependency> nextPhaseIdentifiers, final Set<ModuleIdentifier> nextAlreadySeen) {
        final ModuleResolvePhaseService nextPhaseService = new ModuleResolvePhaseService(moduleIdentifier, nextAlreadySeen, phaseNumber);
        ServiceBuilder<ModuleResolvePhaseService> builder = serviceTarget.addService(moduleSpecServiceName(moduleIdentifier, phaseNumber), nextPhaseService);
        for (ModuleDependency module : nextPhaseIdentifiers) {
            builder.addDependency(module.isOptional() ? OPTIONAL : REQUIRED, ServiceModuleLoader.moduleSpecServiceName(module.getIdentifier()), ModuleDefinition.class, new Injector<ModuleDefinition>() {

                ModuleDefinition definition;

                @Override
                public synchronized void inject(final ModuleDefinition o) throws InjectionException {
                    nextPhaseService.getModuleSpecs().add(o);
                    this.definition = o;
                }

                @Override
                public synchronized void uninject() {
                    nextPhaseService.getModuleSpecs().remove(definition);
                    this.definition = null;
                }
            });
        }
        builder.install();
    }

    @Override
    public void stop(final StopContext stopContext) {

    }

    @Override
    public ModuleResolvePhaseService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Set<ModuleDefinition> getModuleSpecs() {
        return moduleSpecs;
    }

    public static ServiceName moduleSpecServiceName(ModuleIdentifier identifier, int phase) {
        if (!ServiceModuleLoader.isDynamicModule(identifier)) {
            throw ServerMessages.MESSAGES.missingModulePrefix(identifier, ServiceModuleLoader.MODULE_PREFIX);
        }
        return SERVICE_NAME.append(identifier.getName()).append(identifier.getSlot()).append("" + phase);
    }
}
