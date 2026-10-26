# El Plagio (youtuber-ladronMOD)

Mod para Minecraft **1.21.1** (Fabric).

## Qué hace

Si andás con una consola de **RetroCraft** en la mano, al rato aparece en el chat
*"Youtuber porteño Ladrón se unió a la partida"*. Viene caminando, te saca la consola de la mano y te deja **la suya de
EmuMod** (tu GBA por su GBA, tu PS2 por su PS2, tu N64 por su N64...). Las que EmuMod no tiene te las cambia por su
PS3. Después dice algo en el chat y se escapa: si lo matás antes de que se vaya de la partida, recuperás tu consola.

- Sin EmuMod instalado te deja la **Consola Plagiada™** del mod.
- Sin RetroCraft, roba la **Consola Original** del mod (se fabrica con vidrio, hierro y redstone).
- Los cartuchos y discos no le interesan.
- Para que también robe otras cosas, agregalas con un datapack al tag `elplagio:originales`.

Gamerules: `ladronAparece` (true), `ladronDemora` (segundos con la consola en la mano hasta que aparece, 20),
`ladronEspera` (segundos hasta que vuelve a visitar al mismo jugador, 180). También tiene huevo generador.

La skin se puede cambiar poniendo una skin de jugador de 64x64 en `config/elplagio/youtuber_ladron.png`.

## Instalación

Bajá `elplagio-1.0.0.jar` de [Releases](https://github.com/eLGatoRDV/youtuber-ladronMOD/releases).


1. Minecraft **1.21.1** con Fabric Loader y [Fabric API](https://modrinth.com/mod/fabric-api).
2. [RetroCraft](https://github.com/sammwyy/retrocraft/releases) y `elplagio-1.0.0.jar` en la carpeta `mods`.
3. Para que te deje la consola real de EmuMod: EmuMod oficial es para 1.20.1, así que El Plagio trae un **parche** que lo
   convierte para 1.21.1. Abrí `elplagio-1.0.0.jar` con doble click (o `java -jar elplagio-1.0.0.jar`), elegí el
   [`emumod-2.2.0.jar` oficial](https://github.com/Sparretro/EmuMod/releases/tag/v2.2.0) o dejá que lo baje, y la carpeta
   `mods`. Te deja `emumod-2.2.0+1.21.1-parche-elplagio.jar` (y si el oficial estaba en `mods`, lo desactiva).
   También por consola: `java -jar elplagio-1.0.0.jar bajar <carpeta mods>`.

El parche solo trae lo que cambia (~280 KB: clases y datos adaptados a 1.21.1); las DLLs y los modelos salen del jar
oficial, que se verifica por SHA-256. EmuMod emula solo en **Windows**: en macOS y Linux sus consolas se ven, pero no
corren juegos. El parche contiene código derivado de EmuMod (© 2026 Spar Retro, MIT).

## Desarrollo

- `./gradlew build` → `build/libs/elplagio-1.0.0.jar` (Gradle corre con JDK 21 o más nuevo).
- `./gradlew runGametest`: pruebas automáticas. Las del cambiazo con los dos mods corren si ponés RetroCraft y el EmuMod
  parcheado en `libs/` (si no, se saltean).
- `./gradlew generarParcheEmuMod -PjarOficial=... -PjarPorteado=...`: regenera el parche a partir del jar oficial y uno
  porteado a 1.21.1.
- `./gradlew runEscena [-Pconsola=ps2_console]`: abre el juego, arma la escena del cambiazo, saca fotos en
  `run/screenshots/` y se cierra. Con `-Pmodo=mundo` genera `run/saves/ElPlagioVideo`, un mundo para grabar.
