package com.elplagio;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Qué te deja a cambio de la consola que te saca: la suya de EmuMod para la misma consola de RetroCraft (tu GBA por su
 * GBA, tu PS2 por su PS2...). Sin EmuMod instalado te deja la Consola Plagiada del mod.
 */
public final class Cambiazo {
	public static final String EMUMOD = "emumod";
	public static final String RETROCRAFT = "retrocraft";

	// Consola de RetroCraft -> la de EmuMod; las que EmuMod no tiene van a la más parecida
	private static final Map<String, String> SUYA = Map.ofEntries(
			Map.entry("gba", "gba"),
			Map.entry("gba_sp", "gba_sp"),
			Map.entry("gb", "gba"),
			Map.entry("gbc", "gba"),
			Map.entry("nds", "nds"),
			Map.entry("3ds", "n3ds"),
			Map.entry("psp", "psp"),
			Map.entry("nes", "nes"),
			Map.entry("nes_handheld", "nes"),
			Map.entry("snes", "nes"),
			Map.entry("megadrive", "nes"),
			Map.entry("genesis", "nes"),
			Map.entry("n64", "n64"),
			Map.entry("ps1", "ps2_slim"),
			Map.entry("ps2", "ps2"),
			Map.entry("dreamcast", "ps2"),
			Map.entry("gamecube", "wii"),
			Map.entry("wii", "wii"));

	// RetroCraft 0.1.0 (el único release por ahora) nombraba las consolas con sufijo y "gba" era el cartucho; desde
	// después se llaman como el sistema y el cartucho es "gba_cartridge"
	private static final Map<String, String> SUYA_0_1_0 = Map.ofEntries(
			Map.entry("gba_handheld", "gba"),
			Map.entry("gb_handheld", "gba"),
			Map.entry("gbc_handheld", "gba"),
			Map.entry("nes_console", "nes"),
			Map.entry("nes_handheld", "nes"),
			Map.entry("snes_console", "nes"),
			Map.entry("megadrive_console", "nes"),
			Map.entry("genesis_console", "nes"),
			Map.entry("n64_console", "n64"),
			Map.entry("ps1_console", "ps2_slim"),
			Map.entry("ps2_console", "ps2"),
			Map.entry("dreamcast_console", "ps2"),
			Map.entry("gamecube_console", "wii"),
			Map.entry("wii_console", "wii"));

	// Los arcades y la Consola Original del mod: la PS3, la única que RetroCraft no tiene
	private static final String POR_DEFECTO = "ps3";

	private static Boolean nomenclaturaNueva;

	private Cambiazo() {
	}

	private static boolean nomenclaturaNueva() {
		if (nomenclaturaNueva == null) {
			nomenclaturaNueva = BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(RETROCRAFT, "gba_cartridge"));
		}
		return nomenclaturaNueva;
	}

	private static Map<String, String> consolasDeRetroCraft() {
		return nomenclaturaNueva() ? SUYA : SUYA_0_1_0;
	}

	/** Lo que el ladrón te saca de la mano: la Consola Original del mod (o lo que agregues al tag) y las consolas de RetroCraft */
	public static boolean esOriginal(ItemStack stack) {
		return stack.is(ModItems.ORIGINALES) || esConsolaDeRetroCraft(stack);
	}

	// Las consolas, no los cartuchos, discos ni accesorios
	static boolean esConsolaDeRetroCraft(ItemStack stack) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!id.getNamespace().equals(RETROCRAFT)) {
			return false;
		}
		String nombre = id.getPath();
		if (consolasDeRetroCraft().containsKey(nombre)) {
			return true;
		}
		return nomenclaturaNueva()
				? nombre.equals("arcade") || nombre.startsWith("arcade_") && !nombre.equals("arcade_board")
				: nombre.startsWith("mame") && nombre.endsWith("_console");
	}

	/** El ítem de EmuMod que corresponde a esta consola, si EmuMod está instalado */
	public static Optional<Item> suya(ItemStack original) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(original.getItem());
		String suya = id.getNamespace().equals(RETROCRAFT) ? consolasDeRetroCraft().getOrDefault(id.getPath(), POR_DEFECTO) : POR_DEFECTO;
		return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(EMUMOD, suya));
	}

	public static ItemStack plagiar(ItemStack original) {
		Optional<Item> suya = suya(original);
		if (suya.isPresent()) {
			ItemStack plagio = new ItemStack(suya.get());
			plagio.set(DataComponents.LORE, new ItemLore(List.of(
					Component.translatable("tooltip.elplagio.consola_plagiada.0").withStyle(ChatFormatting.LIGHT_PURPLE),
					Component.translatable("tooltip.elplagio.consola_plagiada.1").withStyle(ChatFormatting.LIGHT_PURPLE),
					Component.translatable("tooltip.elplagio.consola_plagiada.2").withStyle(ChatFormatting.LIGHT_PURPLE))));
			return plagio;
		}
		ItemStack plagio = new ItemStack(ModItems.CONSOLA_PLAGIADA);
		if (!original.is(ModItems.CONSOLA_ORIGINAL)) {
			plagio.set(DataComponents.CUSTOM_NAME, Component.translatable("item.elplagio.consola_plagiada.de", original.getHoverName())
					.withStyle(estilo -> estilo.withItalic(false)));
		}
		return plagio;
	}
}
