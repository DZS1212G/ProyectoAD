package servicios;

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
import java.util.List;

/** Trabaja con formato .xlsx (Excel moderno). La fila 1 es la cabecera DATO1..DATO6. */
public class GestorXLS implements IGestorFicheros {

    @Override
    public List<Registro> leer(String rutaArchivo) throws Exception {
        List<Registro> registros = new ArrayList<>();
        File f = new File(rutaArchivo);
        if (!f.exists() || f.length() == 0) {
            return registros;
        }
        DataFormatter fmt = new DataFormatter();
        try (Workbook wb = abrir(f)) {
            Sheet hoja = wb.getSheetAt(0);
            for (int i = 1; i <= hoja.getLastRowNum(); i++) { // i = 1: saltamos la cabecera
                Row fila = hoja.getRow(i);
                if (fila == null) continue;
                registros.add(new Registro(
                        celda(fila, 0, fmt), celda(fila, 1, fmt), celda(fila, 2, fmt),
                        celda(fila, 3, fmt), celda(fila, 4, fmt), celda(fila, 5, fmt)));
            }
        }
        return registros;
    }

    @Override
    public void escribir(String rutaArchivo, Registro reg) throws Exception {
        File f = new File(rutaArchivo);
        try (Workbook wb = abrir(f)) {
            Sheet hoja = wb.getSheetAt(0);
            Row fila = hoja.createRow(hoja.getLastRowNum() + 1);
            String[] datos = {
                reg.getDato1(), reg.getDato2(), reg.getDato3(),
                reg.getDato4(), reg.getDato5(), reg.getDato6()
            };
            for (int i = 0; i < datos.length; i++) {
                fila.createCell(i).setCellValue(datos[i]);
            }
            try (FileOutputStream out = new FileOutputStream(f)) {
                wb.write(out);
            }
        }
    }

    /** Abre el libro existente o crea uno nuevo con la cabecera. */
    private Workbook abrir(File f) throws Exception {
        if (f.exists() && f.length() > 0) {
            try (FileInputStream in = new FileInputStream(f)) {
                return new XSSFWorkbook(in);
            }
        }
        Workbook wb = new XSSFWorkbook();
        Sheet hoja = wb.createSheet("Datos");
        Row cab = hoja.createRow(0);
        for (int i = 0; i < 6; i++) {
            cab.createCell(i).setCellValue("DATO" + (i + 1));
        }
        return wb;
    }

    private String celda(Row fila, int col, DataFormatter fmt) {
        Cell c = fila.getCell(col);
        return c == null ? "" : fmt.formatCellValue(c);
    }
}
