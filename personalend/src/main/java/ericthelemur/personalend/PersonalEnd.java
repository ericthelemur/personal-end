package ericthelemur.personalend;

import ericthelemur.personalend.commands.Commands;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersonalEnd implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("personal-end");
	public static final String MOD_ID = "personalend";


	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var state = DragonPersistentState.getServerState(server);
			state.markDirty();
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			var state = DragonPersistentState.getServerState(server);
			state.markDirty();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			Commands.register(dispatcher);
		});

		LOGGER.info("Initialized Personal End!");
	}
}