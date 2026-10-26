package com.elplagio.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

import com.elplagio.ElPlagio;
import com.elplagio.YoutuberLadron;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/** el youtuber se dibuja con el modelo de jugador (brazos anchos, capas de la skin incluidas) */
public class YoutuberLadronRenderer extends HumanoidMobRenderer<YoutuberLadron, PlayerModel<YoutuberLadron>> {
	private static final ResourceLocation SKIN = ElPlagio.id("textures/entity/youtuber_ladron.png");
	private static final ResourceLocation SKIN_PROPIA = ElPlagio.id("skin_propia/youtuber_ladron");

	@Nullable
	private static ResourceLocation skin;

	public YoutuberLadronRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new PlayerModel<>(contexto.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public void render(YoutuberLadron ladron, float yaw, float tickParcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		// Lo robado lo lleva bien a la vista, con el brazo levantado como un jugador sosteniendo algo
		HumanoidModel.ArmPose brazo = ladron.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
		boolean derecho = ladron.getMainArm() == HumanoidArm.RIGHT;
		getModel().rightArmPose = derecho ? brazo : HumanoidModel.ArmPose.EMPTY;
		getModel().leftArmPose = derecho ? HumanoidModel.ArmPose.EMPTY : brazo;
		super.render(ladron, yaw, tickParcial, pose, buffers, luz);
	}

	@Override
	public ResourceLocation getTextureLocation(YoutuberLadron ladron) {
		if (skin == null) {
			skin = cargarSkin();
		}
		return skin;
	}

	/** config/elplagio/youtuber_ladron.png (una skin de jugador de 64x64) reemplaza la que trae el mod */
	private static ResourceLocation cargarSkin() {
		Path archivo = FabricLoader.getInstance().getConfigDir().resolve("elplagio").resolve("youtuber_ladron.png");
		if (Files.isRegularFile(archivo)) {
			try (InputStream entrada = Files.newInputStream(archivo)) {
				Minecraft.getInstance().getTextureManager().register(SKIN_PROPIA, new DynamicTexture(NativeImage.read(entrada)));
				ElPlagio.LOGGER.info("Skin de el youtuber: {}", archivo);
				return SKIN_PROPIA;
			} catch (IOException e) {
				ElPlagio.LOGGER.warn("No se pudo leer {}, uso la skin del mod", archivo, e);
			}
		}
		return SKIN;
	}
}
