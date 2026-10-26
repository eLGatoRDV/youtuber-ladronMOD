package com.elplagio;

import java.util.EnumSet;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Entra a la partida como si fuera un jugador, va derecho al que tenga una consola original en la mano, se la saca, le
 * deja la suya y se escapa. Si lo matás antes de que se vaya, suelta la consola que te robó.
 */
public class YoutuberLadron extends PathfinderMob {
	/** Cuánto escapa con lo robado antes de irse de la partida */
	static final int TICKS_HUIDA = 20 * 12;
	/** Si vino solo y no hay nada para robar, se aburre y se va */
	private static final int TICKS_ABURRIDO = 20 * 120;
	private static final double RANGO = 32;
	private static final double ALCANCE = 2.2;

	private int huida = -1;
	private int aburrido;
	private boolean deAparicion;
	@Nullable
	private UUID victima;
	private int esperaFrase;

	public YoutuberLadron(EntityType<? extends YoutuberLadron> tipo, Level level) {
		super(tipo, level);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new HuirGoal());
		goalSelector.addGoal(2, new RobarGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	/** Apareció solo para ir a buscar a ese jugador (ver {@link Apariciones}) */
	public void setDeAparicion(UUID victima) {
		this.deAparicion = true;
		this.victima = victima;
	}

	public boolean estaHuyendo() {
		return huida > 0;
	}

	/** La mano en la que el jugador tiene algo que el youtuber quiere, o null */
	@Nullable
	public static InteractionHand manoConOriginal(Player jugador) {
		for (InteractionHand mano : InteractionHand.values()) {
			if (Cambiazo.esOriginal(jugador.getItemInHand(mano))) {
				return mano;
			}
		}
		return null;
	}

	/** El cambiazo: se queda con la original y le deja la plagiada en la misma mano */
	public boolean robar(Player jugador) {
		InteractionHand mano = manoConOriginal(jugador);
		if (mano == null || !getMainHandItem().isEmpty()) {
			return false;
		}
		ItemStack original = jugador.getItemInHand(mano);
		jugador.setItemInHand(mano, Cambiazo.plagiar(original));
		setItemSlot(EquipmentSlot.MAINHAND, original);
		// Mayor que 1: si lo matan la suelta siempre y sin gastar
		setDropChance(EquipmentSlot.MAINHAND, 2.0f);
		swing(InteractionHand.MAIN_HAND);
		victima = jugador.getUUID();
		huida = TICKS_HUIDA;

		if (level() instanceof ServerLevel level) {
			level.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1.0f, 0.8f);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5f, 1.2f);
			level.sendParticles(ParticleTypes.ENCHANT, jugador.getX(), jugador.getY() + 1.2, jugador.getZ(), 40, 0.4, 0.4, 0.4, 0.8);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0, getZ(), 8, 0.3, 0.5, 0.3, 0.0);
		}
		jugador.displayClientMessage(Component.translatable("mensaje.elplagio.robado", getDisplayName(), original.getHoverName())
				.withStyle(ChatFormatting.RED), true);
		decir(Frase.ROBO);
		return true;
	}

	/** Habla en el chat como un jugador: {@code <el youtuber> ...} */
	void decir(Frase frase) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		Component mensaje = Component.translatable("chat.type.text", getDisplayName(), frase.alAzar(random));
		for (ServerPlayer jugador : level.players()) {
			if (jugador.distanceToSqr(this) < 48 * 48) {
				jugador.sendSystemMessage(mensaje);
			}
		}
	}

	/** "el youtuber se unió a la partida" / "salió de la partida", en amarillo como los de verdad */
	private void anunciar(String clave) {
		MinecraftServer server = getServer();
		if (server != null) {
			server.getPlayerList().broadcastSystemMessage(Component.translatable(clave, getDisplayName()).withStyle(ChatFormatting.YELLOW), false);
		}
	}

	private void irse() {
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
		}
		anunciar("mensaje.elplagio.salio");
		discard();
	}

	@Nullable
	private Player getVictima() {
		return victima == null ? null : level().getPlayerByUUID(victima);
	}

	@Nullable
	private Player buscarObjetivo() {
		Player preferido = getVictima();
		if (sirveDeObjetivo(preferido, RANGO * 1.5)) {
			return preferido;
		}
		Player masCerca = null;
		for (Player jugador : level().players()) {
			if (sirveDeObjetivo(jugador, RANGO) && (masCerca == null || distanceToSqr(jugador) < distanceToSqr(masCerca))) {
				masCerca = jugador;
			}
		}
		return masCerca;
	}

	private boolean sirveDeObjetivo(@Nullable Player jugador, double rango) {
		return jugador != null && jugador.isAlive() && !jugador.isSpectator() && manoConOriginal(jugador) != null
				&& distanceToSqr(jugador) < rango * rango;
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance dificultad, MobSpawnType tipo,
			@Nullable SpawnGroupData datos) {
		anunciar("mensaje.elplagio.entro");
		return super.finalizeSpawn(level, dificultad, tipo, datos);
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (esperaFrase > 0) {
			esperaFrase--;
		}
		if (huida > 0) {
			if (--huida == 0) {
				irse();
			}
		} else if (deAparicion && ++aburrido > TICKS_ABURRIDO) {
			decir(Frase.ADIOS);
			irse();
		}
	}

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		boolean herido = super.hurt(fuente, cantidad);
		if (herido && isAlive() && fuente.getEntity() instanceof Player && esperaFrase <= 0) {
			decir(Frase.GOLPE);
			esperaFrase = 60;
		}
		return herido;
	}

	@Override
	public void die(DamageSource fuente) {
		if (!level().isClientSide() && !isRemoved() && !dead) {
			decir(Frase.MUERTE);
			anunciar("mensaje.elplagio.salio");
		}
		super.die(fuente);
	}

	// El nombre siempre arriba de la cabeza, como un jugador
	@Override
	public boolean shouldShowName() {
		return true;
	}

	// Se va él cuando termina; no desaparece por estar lejos
	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Huida", huida);
		tag.putInt("Aburrido", aburrido);
		tag.putBoolean("DeAparicion", deAparicion);
		if (victima != null) {
			tag.putUUID("Victima", victima);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Huida")) {
			huida = tag.getInt("Huida");
		}
		aburrido = tag.getInt("Aburrido");
		deAparicion = tag.getBoolean("DeAparicion");
		victima = tag.hasUUID("Victima") ? tag.getUUID("Victima") : null;
	}

	/** Va hasta el jugador que tiene la consola original y se la saca */
	private class RobarGoal extends Goal {
		@Nullable
		private Player objetivo;
		private int repensar;

		RobarGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (huida >= 0 || !getMainHandItem().isEmpty()) {
				return false;
			}
			objetivo = buscarObjetivo();
			return objetivo != null;
		}

		@Override
		public boolean canContinueToUse() {
			return huida < 0 && sirveDeObjetivo(objetivo, RANGO * 1.5);
		}

		@Override
		public void start() {
			repensar = 0;
			if (esperaFrase <= 0) {
				decir(Frase.VER);
				esperaFrase = 200;
			}
		}

		@Override
		public void stop() {
			objetivo = null;
			getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (objetivo == null) {
				return;
			}
			aburrido = 0;
			getLookControl().setLookAt(objetivo, 30.0f, 30.0f);
			if (distanceToSqr(objetivo) <= ALCANCE * ALCANCE) {
				robar(objetivo);
				return;
			}
			if (--repensar <= 0) {
				repensar = 10;
				getNavigation().moveTo(objetivo, 1.25);
			}
		}
	}

	/** Con la consola en la mano, se aleja de la víctima hasta que se va de la partida */
	private class HuirGoal extends Goal {
		HuirGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return huida > 0;
		}

		@Override
		public void tick() {
			if (huida == TICKS_HUIDA / 2) {
				decir(Frase.HUIDA);
			}
			if (getNavigation().isDone()) {
				Player jugador = getVictima();
				Vec3 lejos = jugador != null
						? DefaultRandomPos.getPosAway(YoutuberLadron.this, 16, 7, jugador.position())
						: DefaultRandomPos.getPos(YoutuberLadron.this, 16, 7);
				if (lejos != null) {
					getNavigation().moveTo(lejos.x, lejos.y, lejos.z, 1.35);
				}
			}
		}
	}
}
