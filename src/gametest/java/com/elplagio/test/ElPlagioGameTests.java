package com.elplagio.test;

import com.elplagio.Apariciones;
import com.elplagio.Cambiazo;
import com.elplagio.ElPlagio;
import com.elplagio.ModEntidades;
import com.elplagio.ModItems;
import com.elplagio.YoutuberLadron;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ElPlagioGameTests implements FabricGameTest {
	private static void piso(GameTestHelper helper, int desde, int hasta) {
		for (int x = desde; x < hasta; x++) {
			for (int z = desde; z < hasta; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	// Con EmuMod instalado es su PS3; sin EmuMod, la Consola Plagiada del mod
	private static Item loQueDejaPorLaOriginal() {
		return Cambiazo.plagiar(new ItemStack(ModItems.CONSOLA_ORIGINAL)).getItem();
	}

	// RetroCraft y EmuMod parcheado van en libs/ (no están en el repo): sin ellos estas pruebas no aplican
	private static boolean faltanLosMods(GameTestHelper helper) {
		if (FabricLoader.getInstance().isModLoaded(Cambiazo.RETROCRAFT) && FabricLoader.getInstance().isModLoaded(Cambiazo.EMUMOD)) {
			return false;
		}
		ElPlagio.LOGGER.warn("Sin RetroCraft y EmuMod en libs/: salteo la prueba del cambiazo con los dos mods");
		helper.succeed();
		return true;
	}

	private static boolean nomenclaturaNueva() {
		return BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(Cambiazo.RETROCRAFT, "gba_cartridge"));
	}

	private static Item item(String mod, String nombre) {
		return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(mod, nombre))
				.orElseThrow(() -> new GameTestAssertException("Falta " + mod + ":" + nombre + " en el entorno de pruebas"));
	}

	// Con RetroCraft y EmuMod instalados: te saca la de sammwy y te da la suya de la misma consola
	@GameTest(template = EMPTY_STRUCTURE)
	public void conLosDosModsTeDaLaSuya(GameTestHelper helper) {
		if (faltanLosMods(helper)) {
			return;
		}
		String[][] casos = nomenclaturaNueva()
				? new String[][] {{"gba", "gba"}, {"ps2", "ps2"}, {"3ds", "n3ds"}, {"n64", "n64"}, {"arcade", "ps3"}}
				: new String[][] {{"gba_handheld", "gba"}, {"ps2_console", "ps2"}, {"n64_console", "n64"}, {"wii_console", "wii"},
						{"mame_console", "ps3"}};
		for (String[] caso : casos) {
			Item deSammwy = item(Cambiazo.RETROCRAFT, caso[0]);
			Item deEl = item(Cambiazo.EMUMOD, caso[1]);
			Player jugador = helper.makeMockPlayer(GameType.SURVIVAL);
			jugador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(deSammwy));
			YoutuberLadron ladron = helper.spawn(ModEntidades.LADRON, 1, 1, 1);

			helper.assertTrue(ladron.robar(jugador), "No le sacó la " + caso[0] + " de RetroCraft");
			helper.assertTrue(jugador.getMainHandItem().is(deEl), "Por la " + caso[0] + " dejó " + jugador.getMainHandItem() + " en vez de emumod:" + caso[1]);
			helper.assertTrue(ladron.getMainHandItem().is(deSammwy), "No se quedó con la " + caso[0]);
			ladron.discard();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void losCartuchosNoLeInteresan(GameTestHelper helper) {
		if (faltanLosMods(helper)) {
			return;
		}
		Item cartucho = item(Cambiazo.RETROCRAFT, nomenclaturaNueva() ? "gba_cartridge" : "gba");
		Player jugador = helper.makeMockPlayer(GameType.SURVIVAL);
		jugador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(cartucho));
		YoutuberLadron ladron = helper.spawn(ModEntidades.LADRON, 1, 1, 1);

		helper.assertFalse(ladron.robar(jugador), "Se llevó un cartucho en vez de una consola");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void teSacaLaConsolaDeLaMano(GameTestHelper helper) {
		Player jugador = helper.makeMockPlayer(GameType.SURVIVAL);
		jugador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CONSOLA_ORIGINAL));
		YoutuberLadron ladron = helper.spawn(ModEntidades.LADRON, 1, 1, 1);

		helper.assertTrue(ladron.robar(jugador), "No hizo el cambiazo");
		helper.assertTrue(jugador.getMainHandItem().is(loQueDejaPorLaOriginal()), "Al jugador no le quedó la plagiada");
		helper.assertTrue(ladron.getMainHandItem().is(ModItems.CONSOLA_ORIGINAL), "el youtuber no tiene la original");
		helper.assertTrue(ladron.estaHuyendo(), "No se escapa con lo robado");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tambienDeLaOtraMano(GameTestHelper helper) {
		Player jugador = helper.makeMockPlayer(GameType.SURVIVAL);
		jugador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
		jugador.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.CONSOLA_ORIGINAL));
		YoutuberLadron ladron = helper.spawn(ModEntidades.LADRON, 1, 1, 1);

		helper.assertTrue(ladron.robar(jugador), "No hizo el cambiazo");
		helper.assertTrue(jugador.getMainHandItem().is(Items.BREAD), "Se llevó el pan");
		helper.assertTrue(jugador.getOffhandItem().is(loQueDejaPorLaOriginal()), "No dejó la plagiada en la otra mano");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void loQueNoEsUnaConsolaNoLeInteresa(GameTestHelper helper) {
		Player jugador = helper.makeMockPlayer(GameType.SURVIVAL);
		jugador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
		YoutuberLadron ladron = helper.spawn(ModEntidades.LADRON, 1, 1, 1);

		helper.assertFalse(ladron.robar(jugador), "Robó algo que no es una consola original");
		helper.assertTrue(jugador.getMainHandItem().is(Items.DIAMOND_SWORD), "Le cambió la espada");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void vaCaminandoHastaVosYTeLaSaca(GameTestHelper helper) {
		piso(helper, -2, 10);
		ServerPlayer jugador = helper.makeMockServerPlayerInLevel();
		jugador.moveTo(helper.absoluteVec(new Vec3(7.5, 1, 7.5)));
		jugador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CONSOLA_ORIGINAL));
		YoutuberLadron ladron = helper.spawn(ModEntidades.LADRON, 0, 1, 0);

		helper.succeedWhen(() -> {
			helper.assertTrue(jugador.getMainHandItem().is(loQueDejaPorLaOriginal()), "Todavía no se la sacó");
			helper.assertTrue(ladron.getMainHandItem().is(ModItems.CONSOLA_ORIGINAL), "Se la sacó otro el youtuber");
			double caminado = ladron.position().distanceTo(helper.absoluteVec(new Vec3(0.5, 1, 0.5)));
			helper.assertTrue(caminado > 6, "No caminó hasta el jugador (" + caminado + " bloques)");
			ElPlagio.LOGGER.info("el youtuber caminó {} bloques y se la sacó en {} ticks", String.format("%.1f", caminado), helper.getTick());
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300, batch = "aparicion")
	public void apareceSiAndasConLaConsola(GameTestHelper helper) {
		piso(helper, -22, 30);
		helper.getLevel().getGameRules().getRule(Apariciones.DEMORA).set(1, helper.getLevel().getServer());
		ServerPlayer jugador = helper.makeMockServerPlayerInLevel();
		jugador.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
		jugador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CONSOLA_ORIGINAL));

		helper.succeedWhen(() -> {
			helper.assertFalse(helper.getLevel().getEntitiesOfClass(YoutuberLadron.class, jugador.getBoundingBox().inflate(64)).isEmpty(),
					"el youtuber no apareció");
			// Que no aparezcan más en los otros tests
			helper.getLevel().getGameRules().getRule(Apariciones.DEMORA).set(20, helper.getLevel().getServer());
		});
	}
}
