package com.elplagio;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/** Lo que dice el youtuber en el chat; los textos están en lang como frase.elplagio.<momento>.<n> */
public enum Frase {
	VER(3),
	ROBO(5),
	HUIDA(2),
	GOLPE(3),
	MUERTE(2),
	ADIOS(1);

	private final int cantidad;

	Frase(int cantidad) {
		this.cantidad = cantidad;
	}

	public Component alAzar(RandomSource azar) {
		return Component.translatable("frase.elplagio." + name().toLowerCase(Locale.ROOT) + "." + azar.nextInt(cantidad));
	}
}
