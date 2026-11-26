package controlador;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Se ejecuta UNA vez cada vez que arranca (o se recarga) la aplicación.
 * Restaura en la carpeta "ficheros" los archivos de muestra de
 * src/main/resources/muestras, de modo que cada inicio parte siempre de los
 * mismos datos y no conserva lo escrito o subido en la ejecución anterior.
 */
@WebListener
public class InicializadorFicheros implements ServletContextListener {

	private static final String[] NOMBRES = { "datos.csv", "datos.json", "datos.xlsx", "datos.xml", "datos.rdf" };

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext ctx = sce.getServletContext();

		// Misma lógica de carpeta que usa ServletFichero
		String rutaWeb = ctx.getRealPath("/ficheros");
		File carpeta = (rutaWeb != null) ? new File(rutaWeb)
				: new File(System.getProperty("java.io.tmpdir"), "ficheros_reto");
		if (!carpeta.exists() && !carpeta.mkdirs()) {
			ctx.log("No se pudo crear la carpeta de ficheros: " + carpeta);
		} else {

			for (String nombre : NOMBRES) {
				File destino = new File(carpeta, nombre);
				try (InputStream in = InicializadorFicheros.class.getResourceAsStream("/muestras/" + nombre)) {
					if (in != null) {
						Files.copy(in, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} else {
						Files.deleteIfExists(destino.toPath()); // sin muestra: empezar vacío
					}
				} catch (Exception e) {
					ctx.log("No se pudo restaurar " + nombre, e);
				}
			}
		}
	}
}
