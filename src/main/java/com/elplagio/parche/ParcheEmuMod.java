package com.elplagio.parche;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Convierte el EmuMod 2.2.0 oficial (Minecraft 1.20.1) en uno para 1.21.1, para usarlo junto a RetroCraft: lo que cambia
 * viene en el parche (clases y datos); las DLLs, los modelos y todo lo demás salen del jar oficial. Corre fuera de
 * Minecraft: {@code java -jar elplagio-1.0.0.jar [emumod-2.2.0.jar|bajar] [carpeta mods]} o doble click.
 */
public final class ParcheEmuMod {
	private static final String RECURSO = "/parche/emumod-2.2.0-a-1.21.1.zip";
	private static final String URL_OFICIAL = "https://github.com/Sparretro/EmuMod/releases/download/v2.2.0/emumod-2.2.0.jar";
	private static final String TITULO = "El Plagio: parche de EmuMod para 1.21.1";

	private ParcheEmuMod() {
	}

	private record Parche(String base, String sha256, String salida, Set<String> borrar, Map<String, byte[]> agregar) {
	}

	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			Path carpeta = args.length > 1 ? Path.of(args[1]) : carpetaDeEsteJar();
			Path oficial = args[0].equals("bajar") ? bajarOficial() : Path.of(args[0]);
			Path hecho = parchear(oficial, carpeta, args[0].equals("bajar"));
			System.out.println("Listo: " + hecho);
		} else if (!GraphicsEnvironment.isHeadless()) {
			SwingUtilities.invokeAndWait(ParcheEmuMod::ventana);
		} else {
			System.out.println("Uso: java -jar elplagio-1.0.0.jar <emumod-2.2.0.jar|bajar> [carpeta mods]");
		}
	}

	/**
	 * Escribe el EmuMod parcheado en {@code carpeta}. Si el oficial estaba en esa misma carpeta (la de mods) lo desactiva,
	 * porque Fabric no arranca con el de 1.20.1; si lo bajó el parcheador, lo borra.
	 */
	public static Path parchear(Path oficial, Path carpeta, boolean borrarOficial) throws IOException {
		Parche parche = leerParche();
		String hash = sha256(oficial);
		if (!hash.equals(parche.sha256())) {
			throw new IOException(oficial.getFileName() + " no es el " + parche.base() + " oficial (SHA-256 " + hash + ")");
		}
		Files.createDirectories(carpeta);
		Path destino = carpeta.resolve(parche.salida());
		Path temporal = carpeta.resolve(parche.salida() + ".tmp");
		try (ZipFile zip = new ZipFile(oficial.toFile()); ZipOutputStream salida = new ZipOutputStream(Files.newOutputStream(temporal))) {
			Enumeration<? extends ZipEntry> entradas = zip.entries();
			while (entradas.hasMoreElements()) {
				ZipEntry entrada = entradas.nextElement();
				String nombre = entrada.getName();
				if (parche.borrar().contains(nombre) || parche.agregar().containsKey(nombre)) {
					continue;
				}
				salida.putNextEntry(new ZipEntry(nombre));
				if (!entrada.isDirectory()) {
					try (InputStream datos = zip.getInputStream(entrada)) {
						datos.transferTo(salida);
					}
				}
				salida.closeEntry();
			}
			for (Map.Entry<String, byte[]> nuevo : parche.agregar().entrySet()) {
				salida.putNextEntry(new ZipEntry(nuevo.getKey()));
				salida.write(nuevo.getValue());
				salida.closeEntry();
			}
		}
		Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
		if (borrarOficial) {
			Files.deleteIfExists(oficial);
		} else if (Files.isSameFile(oficial.toAbsolutePath().getParent(), carpeta)) {
			Files.move(oficial, oficial.resolveSibling(oficial.getFileName() + ".disabled"), StandardCopyOption.REPLACE_EXISTING);
		}
		return destino;
	}

	/** Baja el jar oficial del release de EmuMod en GitHub a un archivo temporal (se verifica al parchear) */
	public static Path bajarOficial() throws IOException, InterruptedException {
		Path temporal = Files.createTempFile("emumod-2.2.0", ".jar");
		HttpClient cliente = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
		HttpResponse<Path> respuesta = cliente.send(HttpRequest.newBuilder(URI.create(URL_OFICIAL)).build(),
				HttpResponse.BodyHandlers.ofFile(temporal));
		if (respuesta.statusCode() != 200) {
			Files.deleteIfExists(temporal);
			throw new IOException("GitHub respondió " + respuesta.statusCode() + " al bajar " + URL_OFICIAL);
		}
		return temporal;
	}

	private static Parche leerParche() throws IOException {
		String base = null, sha256 = null, salida = null;
		Set<String> borrar = new HashSet<>();
		Map<String, byte[]> agregar = new LinkedHashMap<>();
		try (InputStream recurso = ParcheEmuMod.class.getResourceAsStream(RECURSO)) {
			if (recurso == null) {
				throw new IOException("Falta " + RECURSO + " en el jar de El Plagio");
			}
			ZipInputStream zip = new ZipInputStream(recurso);
			for (ZipEntry entrada; (entrada = zip.getNextEntry()) != null;) {
				byte[] datos = zip.readAllBytes();
				if (entrada.getName().equals("parche.txt")) {
					for (String linea : new String(datos, StandardCharsets.UTF_8).split("\n")) {
						int espacio = linea.indexOf(' ');
						if (espacio < 0) {
							continue;
						}
						String valor = linea.substring(espacio + 1);
						switch (linea.substring(0, espacio)) {
							case "base" -> base = valor;
							case "sha256" -> sha256 = valor;
							case "salida" -> salida = valor;
							case "borrar" -> borrar.add(valor);
							default -> {
							}
						}
					}
				} else if (entrada.getName().startsWith("agregar/")) {
					agregar.put(entrada.getName().substring("agregar/".length()), datos);
				}
			}
		}
		if (base == null || sha256 == null || salida == null) {
			throw new IOException("El parche está incompleto");
		}
		return new Parche(base, sha256, salida, borrar, agregar);
	}

	private static String sha256(Path archivo) throws IOException {
		try (DigestInputStream entrada = new DigestInputStream(Files.newInputStream(archivo), MessageDigest.getInstance("SHA-256"))) {
			entrada.transferTo(OutputStream.nullOutputStream());
			return HexFormat.of().formatHex(entrada.getMessageDigest().digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	// Normalmente este jar ya está en la carpeta mods de la instancia
	private static Path carpetaDeEsteJar() {
		try {
			Path jar = Path.of(ParcheEmuMod.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			return Files.isDirectory(jar) ? jar : jar.getParent();
		} catch (URISyntaxException | SecurityException e) {
			return Path.of(".");
		}
	}

	private static void ventana() {
		String[] opciones = {"Elegir emumod-2.2.0.jar", "Bajarlo del GitHub de EmuMod", "Cancelar"};
		int eleccion = JOptionPane.showOptionDialog(null,
				"Convierte EmuMod 2.2.0 oficial (Minecraft 1.20.1) en uno para 1.21.1,\n"
						+ "para usarlo junto a RetroCraft y que el Youtuber porteño Ladrón te deje su consola.\n\n"
						+ "Necesito el emumod-2.2.0.jar oficial: elegilo o lo bajo del release de EmuMod en GitHub.",
				TITULO, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);
		if (eleccion != 0 && eleccion != 1) {
			return;
		}
		Path oficial = null;
		if (eleccion == 0) {
			JFileChooser selector = new JFileChooser(System.getProperty("user.home") + "/Downloads");
			selector.setDialogTitle("Elegí el emumod-2.2.0.jar oficial");
			selector.setFileFilter(new FileNameExtensionFilter("EmuMod (.jar)", "jar"));
			if (selector.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			oficial = selector.getSelectedFile().toPath();
		}
		JFileChooser carpetas = new JFileChooser(carpetaDeEsteJar().toFile());
		carpetas.setDialogTitle("Elegí la carpeta mods de tu instancia 1.21.1");
		carpetas.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		carpetas.setSelectedFile(carpetaDeEsteJar().toFile());
		if (carpetas.showDialog(null, "Usar esta carpeta") != JFileChooser.APPROVE_OPTION) {
			return;
		}
		Path carpeta = carpetas.getSelectedFile().toPath();
		Path elegido = oficial;

		JProgressBar barra = new JProgressBar();
		barra.setIndeterminate(true);
		JDialog trabajando = new JOptionPane(new Object[] {elegido == null ? "Bajando y parcheando EmuMod…" : "Parcheando EmuMod…", barra},
				JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[0]).createDialog(TITULO);
		Thread hilo = new Thread(() -> {
			String mensaje;
			int tipo;
			try {
				Path hecho = elegido == null ? parchear(bajarOficial(), carpeta, true) : parchear(elegido, carpeta, false);
				mensaje = "Listo: " + hecho.getFileName() + "\nen " + carpeta
						+ "\n\nAbrí Minecraft 1.21.1 con Fabric API, RetroCraft, El Plagio y este EmuMod.";
				tipo = JOptionPane.INFORMATION_MESSAGE;
			} catch (Exception e) {
				mensaje = "No se pudo parchear: " + e.getMessage();
				tipo = JOptionPane.ERROR_MESSAGE;
			}
			String texto = mensaje;
			int icono = tipo;
			SwingUtilities.invokeLater(() -> {
				trabajando.dispose();
				JOptionPane.showMessageDialog(null, texto, TITULO, icono);
			});
		}, "parche-emumod");
		hilo.start();
		trabajando.setVisible(true);
	}
}
