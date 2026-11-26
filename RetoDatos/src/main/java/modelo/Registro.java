package modelo;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Registro implements Serializable {
    private Map<String, List<String>> campos = new LinkedHashMap<>();

    @JsonCreator
    public Registro() {}

    public Registro(Map<String, String[]> parameterMap) {
        cargarDesdeParameterMap(parameterMap);
    }

    // Crea campos "dato1", "dato2", ... a partir de un String[]
    public Registro(String[] datosExtraidos) {
        if (datosExtraidos == null) return;
        for (int i = 0; i < datosExtraidos.length; i++) {
            String clave = "dato" + (i + 1);
            String valor = datosExtraidos[i];
            List<String> lista = new ArrayList<>();
            if (valor != null) {
                lista.add(valor);
            }
            this.campos.put(clave, lista);
        }
    }

    // Asocia una cabecera/nombre a cada elemento de datosExtraidos
    public Registro(String[] cabeceras, String[] datosExtraidos) {
        if (datosExtraidos == null) return;
        for (int i = 0; i < datosExtraidos.length; i++) {
            String clave = (cabeceras != null && i < cabeceras.length && cabeceras[i] != null && !cabeceras[i].isBlank())
                    ? cabeceras[i]
                    : "dato" + (i + 1);
            String valor = datosExtraidos[i];
            List<String> lista = new ArrayList<>();
            if (valor != null) {
                lista.add(valor);
            }
            this.campos.put(clave, lista);
        }
    }

    public void cargarDesdeParameterMap(Map<String, String[]> parameterMap) {
        this.campos.clear();
        if (parameterMap == null) return;

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String clave = entry.getKey();
            String[] valores = entry.getValue();
            List<String> lista = (valores != null)
                    ? new ArrayList<>(Arrays.asList(valores))
                    : new ArrayList<>();
            this.campos.put(clave, lista);
        }
    }

    public void setCampo(String clave, List<String> valores) {
        if (clave == null) return;
        this.campos.put(clave, valores != null ? new ArrayList<>(valores) : new ArrayList<>());
    }

    public void setCampo(String clave, String... valores) {
        if (clave == null) return;
        List<String> lista = (valores != null)
                ? new ArrayList<>(Arrays.asList(valores))
                : new ArrayList<>();
        this.campos.put(clave, lista);
    }

    public List<String> getValores(String clave) {
        List<String> lista = campos.get(clave);
        return (lista != null) ? Collections.unmodifiableList(lista) : Collections.emptyList();
    }

    public String getSimple(String clave) {
        List<String> lista = campos.get(clave);
        return (lista != null && !lista.isEmpty()) ? lista.get(0) : null;
    }

    public boolean tieneMultiplesValores(String clave) {
        List<String> lista = campos.get(clave);
        return lista != null && lista.size() > 1;
    }

    public Map<String, List<String>> getCampos() {
        return Collections.unmodifiableMap(campos);
    }

    public void setCampos(Map<String, List<String>> campos) {
        this.campos = new LinkedHashMap<>();
        if (campos != null) {
            for (Map.Entry<String, List<String>> e : campos.entrySet()) {
                setCampo(e.getKey(), e.getValue());
            }
        }
    }
}