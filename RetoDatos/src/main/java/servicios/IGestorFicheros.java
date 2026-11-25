package servicios;

import modelo.Registro;
import java.util.List;

/**
 * Contrato por el que cada tipo de archivo compatible
 * es capaz de leerse y escribirse
 * Por diseño a la hora de implementar un nuevo formato
 * simplemente basta con desarrollar los metodos auxiliares (validacion formateo etc)
 * en la clase que implementa la libreria 
 * 
 */
public interface IGestorFicheros {
    List<Registro> leer(String rutaArchivo) throws Exception;
    void escribir(String rutaArchivo, Registro nuevoRegistro) throws Exception;
}