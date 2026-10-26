package com.elplagio;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntidades {
	// Mismo tamaño que un jugador
	public static final EntityType<YoutuberLadron> LADRON = Registry.register(BuiltInRegistries.ENTITY_TYPE, ElPlagio.id("youtuber_ladron"),
			EntityType.Builder.of(YoutuberLadron::new, MobCategory.MISC)
					.sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10).build("youtuber_ladron"));

	private ModEntidades() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(LADRON, YoutuberLadron.crearAtributos());
	}
}
