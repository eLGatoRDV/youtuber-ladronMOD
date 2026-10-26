package com.elplagio.client;

import com.elplagio.ModEntidades;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ElPlagioClient implements ClientModInitializer {
	private static boolean avisado;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntidades.LADRON, YoutuberLadronRenderer::new);
		// Con RetroCraft pero sin EmuMod el ladrón deja la Consola Plagiada: se avisa una vez cómo tener la de EmuMod
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			FabricLoader loader = FabricLoader.getInstance();
			if (!avisado && loader.isModLoaded("retrocraft") && !loader.isModLoaded("emumod")) {
				avisado = true;
				client.execute(() -> client.player.displayClientMessage(
						Component.translatable("mensaje.elplagio.falta_emumod").withStyle(ChatFormatting.GRAY), false));
			}
		});
	}
}
