package modelo;

import java.util.Arrays;

/**
 * Enum con las posibles acciones
 */
public enum Accion {
	LECTURA,ESCRITURA;
	private static final String[] NAMES = Arrays.stream(values()).map(Enum::name).toArray(String[]::new);

	/**
	 * @return the possible values the enum can become as a String array
	 */
	public static String[] getValues() {
		return NAMES.clone(); // .clone() evita que alguien modifique el array original desde fuera
	}
}
