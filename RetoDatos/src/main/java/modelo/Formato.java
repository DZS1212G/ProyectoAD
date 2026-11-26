package modelo;

import java.util.Arrays;

public enum Formato {
	CSV, JSON, XLS, XLSX, XML, RDF;

	private static final String[] NAMES = Arrays.stream(values()).map(Enum::name).toArray(String[]::new);

	public static String[] getValues() {
		return NAMES.clone(); // .clone() evita que alguien modifique el array original desde fuera
	}
}
