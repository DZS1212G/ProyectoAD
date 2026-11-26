package modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Una fila del formulario dinámico: una clave con uno o más valores.
 * Es solo el "estado de pantalla" para volver a pintar el formulario tras pulsar
 * "+ valor", "×", etc. (no es lo que se guarda en el fichero: eso es Registro).
 */
public class FilaFormulario implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private String clave;
    private final List<String> valores = new ArrayList<>();

    public FilaFormulario(int id, String clave, List<String> valores) {
        this.id = id;
        this.clave = clave == null ? "" : clave;
        if (valores != null) {
            this.valores.addAll(valores);
        }
        if (this.valores.isEmpty()) {
            this.valores.add(""); // toda fila tiene siempre al menos un campo de valor
        }
    }

    public int getId() { return id; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave == null ? "" : clave; }

    /** Lista modificable: el servlet añade/quita valores directamente. */
    public List<String> getValores() { return valores; }
}
