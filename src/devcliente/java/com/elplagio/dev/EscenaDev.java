package com.elplagio.dev;

import java.util.Comparator;

import com.elplagio.Apariciones;
import com.elplagio.ElPlagio;
import com.elplagio.ModEntidades;
import com.elplagio.YoutuberLadron;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;

/**
 * Solo para desarrollo (./gradlew runEscena): crea un mundo, te pone una consola de RetroCraft en la mano, hace aparecer
 * al ladrón enfrente, saca fotos del cambiazo en run/screenshots/ y cierra el juego.
 */
public class EscenaDev implements ClientModInitializer {
	private static final int LIMITE = 20 * 60;

	private boolean mundoPedido;
	private int ticks;
	private int robadoEn = -1;

	@Override
	public void onInitializeClient() {
		if (System.getProperty("elplagio.dev.modo", "fotos").equals("fotos")) {
			ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		}
	}

	private void tick(Minecraft mc) {
		if (!mundoPedido) {
			if (mc.screen != null && mc.getOverlay() == null) {
				mundoPedido = true;
				mc.createWorldOpenFlows().createFreshLevel("elplagio_dev_" + System.currentTimeMillis(),
						new LevelSettings("El Plagio dev", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
								WorldDataConfiguration.DEFAULT),
						new WorldOptions(1234, false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
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
		YoutuberLadron ladron = mc.level.getEntitiesOfClass(YoutuberLadron.class, mc.player.getBoundingBox().inflate(32)).stream()
				.min(Comparator.comparingDouble(mc.player::distanceToSqr)).orElse(null);
		if (ladron != null && robadoEn < 0) {
			mirar(mc, ladron.getEyePosition());
		}
		if (ticks == 60) {
			foto(mc, "1_aparece");
		}
		boolean robado = BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).getNamespace().equals("emumod");
		if (robado && robadoEn < 0) {
			robadoEn = ticks;
			ElPlagio.LOGGER.info("Escena: cambiazo a los {} ticks, ahora tengo {}", ticks, mc.player.getMainHandItem());
		}
		if (robadoEn >= 0 && ticks == robadoEn + 4) {
			foto(mc, "2_cambiazo");
		}
		if (robadoEn >= 0 && ticks == robadoEn + 50) {
			foto(mc, "3_se_escapa");
		}
		if (robadoEn >= 0 && ticks == robadoEn + 60 || ticks > LIMITE) {
			if (robadoEn < 0) {
				foto(mc, "fallo");
				ElPlagio.LOGGER.error("Escena: no hubo cambiazo en {} ticks", LIMITE);
			}
			mc.stop();
		}
	}

	// Mediodía, sin el ladrón automático; la consola en la mano y el ladrón a 7 bloques, enfrente
	private static void preparar(MinecraftServer server) {
		server.execute(() -> {
			ServerPlayer jugador = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = jugador.serverLevel();
			level.setDayTime(6000);
			level.getGameRules().getRule(Apariciones.APARECE).set(false, server);
			level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
			String consola = System.getProperty("elplagio.dev.consola", "gba_handheld");
			ItemStack enLaMano = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("retrocraft", consola)));
			jugador.setItemInHand(InteractionHand.MAIN_HAND, enLaMano);
			Vec3 frente = Vec3.directionFromRotation(0, jugador.getYRot()).scale(7);
			BlockPos lugar = BlockPos.containing(jugador.position().add(frente));
			lugar = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lugar);
			ModEntidades.LADRON.spawn(level, lugar, MobSpawnType.COMMAND);
			ElPlagio.LOGGER.info("Escena: {} en la mano, ladrón en {}", enLaMano, lugar);
		});
	}

	private static void mirar(Minecraft mc, Vec3 punto) {
		Vec3 d = punto.subtract(mc.player.getEyePosition());
		mc.player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
		mc.player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
	}

	private static void foto(Minecraft mc, String nombre) {
		Screenshot.grab(mc.gameDirectory, "elplagio_" + nombre + ".png", mc.getMainRenderTarget(), mensaje -> {
		});
		ElPlagio.LOGGER.info("Escena: foto {}", nombre);
	}
}
