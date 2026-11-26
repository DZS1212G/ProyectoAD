package servicios;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import modelo.Registro;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GestorXLS implements IGestorFicheros {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<Registro> leer(String rutaArchivo) throws Exception {
        List<Registro> registros = new ArrayList<>();
        File f = new File(rutaArchivo);
        if (!f.exists() || f.length() == 0) {
            return registros;
        }

        DataFormatter fmt = new DataFormatter();
        try (Workbook wb = abrirLibroExistente(f)) {
            Sheet hoja = wb.getSheetAt(0);
            Row filaCabecera = hoja.getRow(0);
            if (filaCabecera == null) {
                return registros;
            }

            List<String> cabeceras = new ArrayList<>();
            for (int c = 0; c < filaCabecera.getLastCellNum(); c++) {
                cabeceras.add(celda(filaCabecera, c, fmt));
            }

            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) continue;

                Map<String, List<String>> mapa = new LinkedHashMap<>();
                for (int c = 0; c < cabeceras.size(); c++) {
                    String clave = cabeceras.get(c);
                    if (clave == null || clave.isBlank()) continue;
                    String raw = celda(fila, c, fmt);
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

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Datos");

            Row cab = hoja.createRow(0);
            for (int c = 0; c < columnas.size(); c++) {
                cab.createCell(c).setCellValue(columnas.get(c));
            }

            for (int i = 0; i < existentes.size(); i++) {
                Registro r = existentes.get(i);
                Row fila = hoja.createRow(i + 1);
                for (int c = 0; c < columnas.size(); c++) {
                    String col = columnas.get(c);
                    List<String> valores = r.getValores(col);
                    fila.createCell(c).setCellValue(mapper.writeValueAsString(valores));
                }
            }

            try (FileOutputStream out = new FileOutputStream(f)) {
                wb.write(out);
            }
        }
    }

    private Workbook abrirLibroExistente(File f) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            return new XSSFWorkbook(in);
        }
    }

    private String celda(Row fila, int col, DataFormatter fmt) {
        Cell c = fila.getCell(col);
        return c == null ? "" : fmt.formatCellValue(c);
    }

    private List<String> parsearCelda(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            List<String> fallback = new ArrayList<>();
            fallback.add(raw);
            return fallback;
        }
    }
}