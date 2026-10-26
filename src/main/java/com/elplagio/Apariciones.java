package com.elplagio;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

/** el youtuber se entera cuando alguien anda con una consola original en la mano y entra a la partida a buscarla */
public final class Apariciones {
	public static final GameRules.Key<GameRules.BooleanValue> APARECE = GameRuleRegistry.register("ladronAparece",
			GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));

	/** Segundos seguidos con la consola en la mano hasta que aparece */
	public static final GameRules.Key<GameRules.IntegerValue> DEMORA = GameRuleRegistry.register("ladronDemora",
			GameRules.Category.MOBS, GameRuleFactory.createIntRule(20, 0));

	/** Segundos hasta que puede volver a visitar al mismo jugador */
	public static final GameRules.Key<GameRules.IntegerValue> ESPERA = GameRuleRegistry.register("ladronEspera",
			GameRules.Category.MOBS, GameRuleFactory.createIntRule(180, 0));

	private static final Map<UUID, Integer> SEGUNDOS_CON_ORIGINAL = new HashMap<>();
	private static final Map<UUID, Long> ULTIMA_VISITA = new HashMap<>();

	private Apariciones() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(Apariciones::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SEGUNDOS_CON_ORIGINAL.clear();
			ULTIMA_VISITA.clear();
		});
	}

	private static void tick(ServerLevel level) {
		if (level.getGameTime() % 20 != 0 || !level.getGameRules().getBoolean(APARECE)) {
			return;
		}
		for (ServerPlayer jugador : level.players()) {
			UUID uuid = jugador.getUUID();
			if (jugador.isSpectator() || YoutuberLadron.manoConOriginal(jugador) == null) {
				SEGUNDOS_CON_ORIGINAL.remove(uuid);
				continue;
			}
			int segundos = SEGUNDOS_CON_ORIGINAL.merge(uuid, 1, Integer::sum);
			if (segundos < level.getGameRules().getInt(DEMORA)) {
				continue;
			}
			Long ultima = ULTIMA_VISITA.get(uuid);
			if (ultima != null && level.getGameTime() - ultima < level.getGameRules().getInt(ESPERA) * 20L) {
				continue;
			}
			if (!level.getEntitiesOfClass(YoutuberLadron.class, jugador.getBoundingBox().inflate(64)).isEmpty()) {
				continue;
			}
			if (aparecerCerca(level, jugador)) {
				ULTIMA_VISITA.put(uuid, level.getGameTime());
				SEGUNDOS_CON_ORIGINAL.remove(uuid);
			}
		}
	}

	/** Lo pone a 10-18 bloques del jugador, en un lugar donde entre parado */
	public static boolean aparecerCerca(ServerLevel level, ServerPlayer jugador) {
		RandomSource azar = level.getRandom();
		for (int intento = 0; intento < 16; intento++) {
			double angulo = azar.nextDouble() * Math.PI * 2;
			double distancia = 10 + azar.nextDouble() * 8;
			int x = Mth.floor(jugador.getX() + Math.cos(angulo) * distancia);
			int z = Mth.floor(jugador.getZ() + Math.sin(angulo) * distancia);
			BlockPos lugar = lugarValido(level, x, Mth.floor(jugador.getY()), z);
			if (lugar == null) {
				continue;
			}
			YoutuberLadron ladron = ModEntidades.LADRON.create(level);
			if (ladron == null) {
				return false;
			}
			ladron.moveTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, azar.nextFloat() * 360.0f, 0.0f);
			ladron.setDeAparicion(jugador.getUUID());
			ladron.finalizeSpawn(level, level.getCurrentDifficultyAt(lugar), MobSpawnType.EVENT, null);
			level.addFreshEntity(ladron);
			return true;
		}
		return false;
	}

	// Primero cerca de la altura del jugador (cuevas, casas), después la superficie
	@Nullable
	private static BlockPos lugarValido(ServerLevel level, int x, int yJugador, int z) {
		for (int dy = 0; dy <= 6; dy++) {
			for (int signo : new int[] {1, -1}) {
				BlockPos lugar = new BlockPos(x, yJugador + dy * signo, z);
				if (sirve(level, lugar)) {
					return lugar;
				}
			}
		}
		if (!level.isLoaded(new BlockPos(x, yJugador, z))) {
			return null;
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos superficie = new BlockPos(x, y, z);
		return Math.abs(y - yJugador) < 24 && sirve(level, superficie) ? superficie : null;
	}

	private static boolean sirve(ServerLevel level, BlockPos lugar) {
		BlockPos abajo = lugar.below();
		return level.isLoaded(lugar)
				&& level.getBlockState(abajo).isFaceSturdy(level, abajo, Direction.UP)
				&& level.getFluidState(lugar).isEmpty()
				&& level.noCollision(ModEntidades.LADRON.getSpawnAABB(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5));
	}
}
