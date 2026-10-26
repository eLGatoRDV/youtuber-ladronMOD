package com.elplagio.dev;

import java.util.List;

import com.elplagio.Apariciones;
import com.elplagio.ElPlagio;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Solo para desarrollo (./gradlew runEscena -Pmodo=mundo): genera run/saves/ElPlagioVideo, un mundo creativo para grabar
 * el video, con un cofre lleno de consolas de RetroCraft al lado del spawn y el ladrón apurado. Lo guarda y cierra.
 */
public class MundoVideoDev implements ClientModInitializer {
	static final String CARPETA = "ElPlagioVideo";

	// Las consolas de RetroCraft 0.1.0 (el release que se instala)
	private static final List<String> CONSOLAS = List.of("gba_handheld", "gb_handheld", "gbc_handheld", "nes_handheld", "nes_console",
			"snes_console", "n64_console", "ps1_console", "ps2_console", "wii_console", "gamecube_console", "dreamcast_console",
			"megadrive_console", "genesis_console", "mame_console");

	private boolean mundoPedido;
	private int ticks;
	private boolean guardado;

	@Override
	public void onInitializeClient() {
		if (System.getProperty("elplagio.dev.modo", "fotos").equals("mundo")) {
			ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		}
	}

	private void tick(Minecraft mc) {
		if (guardado) {
			mc.stop();
			return;
		}
		if (!mundoPedido) {
			if (mc.screen != null && mc.getOverlay() == null) {
				mundoPedido = true;
				mc.createWorldOpenFlows().createFreshLevel(CARPETA,
						new LevelSettings("El Plagio - Video", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
								WorldDataConfiguration.DEFAULT),
						new WorldOptions(1234, true, false), WorldPresets::createNormalWorldDimensions, mc.screen);
			}
			return;
		}
		if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.screen != null) {
			return;
		}
		ticks++;
		if (ticks == 40) {
			preparar(mc.getSingleplayerServer());
		}
		if (ticks == 100) {
			// Como "Guardar y salir" del menú de pausa
			mc.level.disconnect();
			mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
			guardado = true;
			ElPlagio.LOGGER.info("Mundo de video guardado en saves/{}", CARPETA);
		}
	}

	private static void preparar(MinecraftServer server) {
		server.execute(() -> {
			ServerPlayer jugador = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = jugador.serverLevel();
			GameRules reglas = level.getGameRules();
			reglas.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
			reglas.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
			reglas.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
			reglas.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, server);
			reglas.getRule(Apariciones.APARECE).set(true, server);
			reglas.getRule(Apariciones.DEMORA).set(6, server);
			reglas.getRule(Apariciones.ESPERA).set(30, server);
			level.setDayTime(6000);
			level.setWeatherParameters(1_000_000, 0, false, false);

			BlockPos spawn = lugarAbierto(level, jugador.blockPosition());
			level.setDefaultSpawnPos(spawn, 0.0f);
			// En un mundo de un jugador el que lo abre usa estos datos (level.dat): parado en el spawn, mirando el cofre
			jugador.getInventory().clearContent();
			jugador.teleportTo(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0f, 20.0f);

			// Mirando al sur desde el spawn: el cofre a dos bloques y el cartel arriba
			BlockPos cofre = spawn.south(2);
			level.setBlockAndUpdate(cofre, Blocks.CHEST.defaultBlockState());
			if (level.getBlockEntity(cofre) instanceof ChestBlockEntity caja) {
				int slot = 0;
				for (String consola : CONSOLAS) {
					Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("retrocraft", consola));
					if (item != Blocks.AIR.asItem()) {
						caja.setItem(slot++, new ItemStack(item));
					}
				}
				ElPlagio.LOGGER.info("Cofre con {} consolas de RetroCraft en {}", slot, cofre);
			}
			BlockPos cartel = cofre.above();
			level.setBlockAndUpdate(cartel, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8));
			if (level.getBlockEntity(cartel) instanceof SignBlockEntity letrero) {
				letrero.setText(new SignText()
						.setMessage(0, Component.literal("Consolas"))
						.setMessage(1, Component.literal("originales"))
						.setMessage(2, Component.literal("(de sammwy)"))
						.setMessage(3, Component.literal("¡cuidalas!")), true);
			}
		});
	}

	// Pasto con el terreno parejo alrededor, cerca de donde arrancó el mundo
	private static BlockPos lugarAbierto(ServerLevel level, BlockPos inicio) {
		for (int radio = 0; radio < 64; radio += 2) {
			for (int dx = -radio; dx <= radio; dx += 2) {
				for (int dz = -radio; dz <= radio; dz += 2) {
					BlockPos superficie = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, inicio.offset(dx, 0, dz));
					if (level.getBlockState(superficie.below()).is(Blocks.GRASS_BLOCK) && parejo(level, superficie)) {
						return superficie;
					}
				}
			}
		}
		return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, inicio);
	}

	private static boolean parejo(ServerLevel level, BlockPos centro) {
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 6; dz++) {
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, centro.getX() + dx, centro.getZ() + dz);
				if (Math.abs(y - centro.getY()) > 1) {
					return false;
				}
			}
		}
		return true;
	}
}
