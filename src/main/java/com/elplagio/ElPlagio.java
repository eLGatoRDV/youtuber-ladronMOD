package com.elplagio;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElPlagio implements ModInitializer {
	public static final String MOD_ID = "elplagio";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntidades.register();
		ModItems.register();
		Apariciones.register();
		LOGGER.info("El Plagio inicializado: cuidá tu consola");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
