package org.eqasim.switzerland;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;

import ch.ethz.idsc.amodeus.data.LocationSpec;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusDatabaseModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusDispatcherModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusRouterModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVehicleGeneratorModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVehicleToVSGeneratorModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVirtualNetworkModule;
import ch.ethz.idsc.amodeus.net.DatabaseModule;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.matsim.av.framework.AVModule;
import luc.simulationTask.LucRealTimeTravelTimemodule;
import scenario.modules.IDSCDispatcherModule;
import scenario.modules.IDSCVehicleGeneratorModule;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), Configurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);

		Configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		Configurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		Configurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));
		
        // New added from AMoDeus starts here:
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        Network network = scenario.getNetwork();
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);

        controller.addOverridingModule(new LucRealTimeTravelTimemodule());
        controller.addOverridingModule(new AVModule());
        controller.addOverridingModule(new AmodeusRouterModule());
        controller.addOverridingModule(new DatabaseModule());
        controller.addOverridingModule(new AmodeusVehicleGeneratorModule());
        controller.addOverridingModule(new IDSCVehicleGeneratorModule());
        controller.addOverridingModule(new AmodeusVirtualNetworkModule(scenarioOptions));
        controller.addOverridingModule(new AmodeusDatabaseModule(db));
        controller.addOverridingModule(new IDSCDispatcherModule());
        controller.addOverridingModule(new AmodeusDispatcherModule());
        controller.addOverridingModule(new AmodeusVehicleToVSGeneratorModule());
        // ===============================================
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(Key.get(Network.class, Names.named("dvrp_routing"))).to(Network.class);
            }
        });
        controller.addOverridingModule(new AmodeusModule());

        // New added ends here

		

		controller.run();
	}
}
