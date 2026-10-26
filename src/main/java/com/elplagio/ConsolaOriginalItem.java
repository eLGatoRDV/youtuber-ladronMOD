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

public class ConsolaOriginalItem extends Item {
	public ConsolaOriginalItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> lineas, TooltipFlag flag) {
		for (int i = 0; i < 3; i++) {
			lineas.add(Component.translatable("tooltip.elplagio.consola_original." + i).withStyle(ChatFormatting.GRAY));
		}
		lineas.add(Component.translatable("tooltip.elplagio.consola_original.aviso").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		if (!level.isClientSide()) {
			jugador.displayClientMessage(Component.translatable("mensaje.elplagio.consola_original.usar").withStyle(ChatFormatting.GREEN), true);
		}
		return InteractionResultHolder.sidedSuccess(jugador.getItemInHand(mano), level.isClientSide());
	}
}
