/*
 * Copyright 2012-2016, the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.flux.initializer;

import akka.actor.ActorRef;
import com.flipkart.flux.guice.module.ConfigModule;
import com.flipkart.flux.guice.module.ContainerModule;
import com.flipkart.flux.guice.module.HibernateModule;
import com.flipkart.flux.impl.boot.TaskModule;
import com.flipkart.flux.impl.task.registry.EagerInitRouterRegistryImpl;
import com.flipkart.flux.impl.temp.Work;
import com.flipkart.polyguice.core.support.Polyguice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;

import java.net.URL;

import static com.flipkart.flux.constant.RuntimeConstants.CONFIGURATION_YML;

/**
 * <code>FluxInitializer</code> is the initializer class which starts jetty server and loads polyguice container.
 * @author shyam.akirala
 */
public class FluxInitializer {

    private static final Logger logger = LogManager.getLogger(FluxInitializer.class);

    private Polyguice fluxRuntimeContainer;
    private final URL configUrl;

    public FluxInitializer(String config) {
        configUrl = this.getClass().getClassLoader().getResource(config);
        this.fluxRuntimeContainer = new Polyguice();
    }


    public static void main(String[] args) throws Exception {
        String command = "start";
        String config  = CONFIGURATION_YML;
        if (args != null && args.length> 0) {
            command = args[0];
        }
        final FluxInitializer fluxInitializer = new FluxInitializer(config);
        switch (command) {
            case "start" :
                fluxInitializer.start();
                break;
            case "migrate" :
                fluxInitializer.migrate();
                break;
        }
    }

    private void loadFluxRuntimeContainer() {
        logger.debug("loading flux runtime container");
        final ConfigModule configModule = new ConfigModule(configUrl);
        fluxRuntimeContainer.modules(configModule, new HibernateModule(), new ContainerModule(), new TaskModule());
        fluxRuntimeContainer.registerConfigurationProvider(configModule.getConfigProvider());
        fluxRuntimeContainer.prepare();
    }

    private void start() throws Exception {
        //load flux runtime container
        loadFluxRuntimeContainer();
        initialiseAkkaRuntime(fluxRuntimeContainer);
        logger.debug("loading API server");
        final Server apiJettyServer = fluxRuntimeContainer.getComponentContext().getInstance("APIJettyServer", Server.class);
        apiJettyServer.start();
        logger.debug("API server started. Say Hello!");

        logger.debug("Loading Dashboard Server");
        final Server dashboardJettyServer = fluxRuntimeContainer.getComponentContext().getInstance("DashboardJettyServer", Server.class);
        dashboardJettyServer.start();
        logger.debug("Dashboard server has started. Say Hello!");
        testOut();
    }

    // TODO (Temporary) - For Mr Regunath to play with :)
    private void testOut() throws InterruptedException {
        final EagerInitRouterRegistryImpl routerRegistry = fluxRuntimeContainer.getComponentContext().getInstance(EagerInitRouterRegistryImpl.class);
        routerRegistry.getRouter("someRouter").tell("Message for some router", ActorRef.noSender());
        routerRegistry.getRouter("someRouterWithoutConfig").tell("Message for some router with no config", ActorRef.noSender());
        Thread.sleep(1000l);
        for (int i = 0 ; i < 10000 ; i++) {
            routerRegistry.getRouter("someRouter").tell(new Work(),ActorRef.noSender());
        }
    }

    private void initialiseAkkaRuntime(Polyguice polyguice) throws InterruptedException {
        final EagerInitRouterRegistryImpl routerRegistry = polyguice.getComponentContext().getInstance(EagerInitRouterRegistryImpl.class); // This basically "inits" the system. Will be handled by PolyTrooper
        Thread.sleep(1000l); // TODO Booo, do something about this
    }

    private void migrate() {
        loadFluxRuntimeContainer();
//        MigrationsRunner migrationsRunner = (MigrationsRunner) fluxRuntimeContainer.getInstanceOfClass(MigrationsRunner.class);
//        migrationsRunner.migrate();
        // TODO needs to be fixed
    }
}