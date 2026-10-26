package com.elplagio;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ConsolaPlagiadaItem extends Item {
	private static final int MENSAJES_USAR = 3;

	public ConsolaPlagiadaItem(Properties propiedades) {
		super(propiedades);
	}

	// Brilla como encantada: "mejores visuales"
	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> lineas, TooltipFlag flag) {
		for (int i = 0; i < 3; i++) {
			lineas.add(Component.translatable("tooltip.elplagio.consola_plagiada." + i).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		if (!level.isClientSide()) {
			String clave = "mensaje.elplagio.consola_plagiada.usar." + level.getRandom().nextInt(MENSAJES_USAR);
			jugador.displayClientMessage(Component.translatable(clave).withStyle(ChatFormatting.LIGHT_PURPLE), true);
		}
		return InteractionResultHolder.sidedSuccess(jugador.getItemInHand(mano), level.isClientSide());
	}
}
