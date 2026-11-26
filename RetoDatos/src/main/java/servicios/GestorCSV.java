package servicios;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import modelo.Registro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GestorCSV implements IGestorFicheros {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<Registro> leer(String rutaArchivo) throws Exception {
        List<Registro> registros = new ArrayList<>();
        File f = new File(rutaArchivo);
        if (!f.exists() || f.length() == 0) {
            return registros;
        }

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {

            String[] cabeceras = reader.readNext();
            if (cabeceras == null || cabeceras.length == 0) {
                return registros;
            }

            String[] linea;
            while ((linea = reader.readNext()) != null) {
                Map<String, List<String>> mapa = new LinkedHashMap<>();
                for (int i = 0; i < cabeceras.length; i++) {
                    String clave = cabeceras[i];
                    String raw = (i < linea.length) ? linea[i] : "";
                    mapa.put(clave, parsearCelda(raw));
                }
                Registro r = new Registro();
                r.setCampos(mapa);
                registros.add(r);
            }
        }
        return registros;
    }

    @Override
    public void escribir(String rutaArchivo, Registro nuevoRegistro) throws Exception {
        List<Registro> existentes = leer(rutaArchivo);
        existentes.add(nuevoRegistro);

        Set<String> columnasSet = new LinkedHashSet<>();
        for (Registro r : existentes) {
            columnasSet.addAll(r.getCampos().keySet());
        }
        List<String> columnas = new ArrayList<>(columnasSet);

        File f = new File(rutaArchivo);
        if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }

        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(new FileOutputStream(f, false), StandardCharsets.UTF_8))) {

            writer.writeNext(columnas.toArray(new String[0]));

            for (Registro r : existentes) {
                String[] fila = new String[columnas.size()];
                for (int i = 0; i < columnas.size(); i++) {
                    String col = columnas.get(i);
                    List<String> valores = r.getValores(col);
                    fila[i] = mapper.writeValueAsString(valores);
                }
                writer.writeNext(fila);
            }
        }
    }

    private List<String> parsearCelda(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // Compatibilidad por si alguna fila antigua contiene texto plano
            List<String> fallback = new ArrayList<>();
            fallback.add(raw);
            return fallback;
        }
    }
}