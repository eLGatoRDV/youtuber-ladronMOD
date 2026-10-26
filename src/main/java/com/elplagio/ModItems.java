package com.elplagio;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	// Para jugar sin RetroCraft: la "original" que se puede fabricar
	public static final Item CONSOLA_ORIGINAL = registrar("consola_original",
			new ConsolaOriginalItem(new Item.Properties().stacksTo(1)));

	// Solo se consigue cuando el youtuber te la cambia por la tuya
	public static final Item CONSOLA_PLAGIADA = registrar("consola_plagiada",
			new ConsolaPlagiadaItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

	public static final Item HUEVO_LADRON = registrar("youtuber_ladron_spawn_egg",
			new SpawnEggItem(ModEntidades.LADRON, 0x2F6FDB, 0xF2F2F2, new Item.Properties()));

	/** Más cosas para que te saque de la mano, además de las consolas de RetroCraft (ver {@link Cambiazo#esOriginal}) */
	public static final TagKey<Item> ORIGINALES = TagKey.create(Registries.ITEM, ElPlagio.id("originales"));

	private ModItems() {
	}

	private static Item registrar(String nombre, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, ElPlagio.id(nombre), item);
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(CONSOLA_ORIGINAL);
			entries.accept(CONSOLA_PLAGIADA);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(HUEVO_LADRON));
	}
}
