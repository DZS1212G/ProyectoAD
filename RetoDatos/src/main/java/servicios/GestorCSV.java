package servicios;

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
import java.util.List;

public class GestorCSV implements IGestorFicheros {

    @Override
    public List<Registro> leer(String rutaArchivo) throws Exception {
        List<Registro> registros = new ArrayList<>();
        File f = new File(rutaArchivo);
        if (!f.exists() || f.length() == 0) {
            return registros; // aún no hay datos
        }
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String[] linea;
            while ((linea = reader.readNext()) != null) {
                if (linea.length >= 6) {
                    registros.add(new Registro(linea[0], linea[1], linea[2], linea[3], linea[4], linea[5]));
                }
            }
        }
        return registros;
    }

    @Override
    public void escribir(String rutaArchivo, Registro reg) throws Exception {
        // append = true: añade al final sin borrar lo anterior
        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(new FileOutputStream(rutaArchivo, true), StandardCharsets.UTF_8))) {
            writer.writeNext(new String[] {
                reg.getDato1(), reg.getDato2(), reg.getDato3(),
                reg.getDato4(), reg.getDato5(), reg.getDato6()
            });
        }
    }
}
