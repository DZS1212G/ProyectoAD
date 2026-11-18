package entity;


import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GestionXLS {

    public static List<DatosODS> leer(String rutaFichero) throws IOException {
        List<DatosODS> lista = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(rutaFichero);
             Workbook workbook = new HSSFWorkbook(fis)) {

            Sheet hoja = workbook.getSheetAt(0);
            // Empezamos en i = 1 para saltar la fila de encabezados (fila 0)
            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) continue;

                String d1 = formatter.formatCellValue(fila.getCell(0));
                String d2 = formatter.formatCellValue(fila.getCell(1));
                String d3 = formatter.formatCellValue(fila.getCell(2));
                String d4 = formatter.formatCellValue(fila.getCell(3));
                String d5 = formatter.formatCellValue(fila.getCell(4));
                String d6 = formatter.formatCellValue(fila.getCell(5));

                lista.add(new DatosODS(d1, d2, d3, d4, d5, d6));
            }
        }
        return lista;
    }

    public static void escribir(String rutaFichero, DatosODS nuevoDato) throws IOException {
        File archivo = new File(rutaFichero);
        Workbook workbook;
        Sheet hoja;

        if (archivo.exists() && archivo.length() > 0) {
            try (FileInputStream fis = new FileInputStream(archivo)) {
                workbook = new HSSFWorkbook(fis);
            }
            hoja = workbook.getSheetAt(0);
        } else {
            workbook = new HSSFWorkbook();
            hoja = workbook.createSheet("DatosODS");
            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < 6; i++) {
                cabecera.createCell(i).setCellValue("DATO" + (i + 1));
            }
        }

        int nuevaFilaIdx = hoja.getLastRowNum() + 1;
        Row fila = hoja.createRow(nuevaFilaIdx);

        fila.createCell(0).setCellValue(nuevoDato.getDato1());
        fila.createCell(1).setCellValue(nuevoDato.getDato2());
        fila.createCell(2).setCellValue(nuevoDato.getDato3());
        fila.createCell(3).setCellValue(nuevoDato.getDato4());
        fila.createCell(4).setCellValue(nuevoDato.getDato5());
        fila.createCell(5).setCellValue(nuevoDato.getDato6());

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            workbook.write(fos);
        } finally {
            workbook.close();
        }
    }
}