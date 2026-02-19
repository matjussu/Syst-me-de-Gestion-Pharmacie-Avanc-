package com.sgpa.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Utilitaire pour la generation de fichiers Excel (.xlsx) avec Apache POI.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class ExcelExporter {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExporter.class);

    private static final String DEFAULT_OUTPUT_DIR = System.getProperty("user.home") + "/ApotiCare_Exports";
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Couleurs brand
    private static final byte[] COLOR_GREEN = new byte[]{22, 101, 52};
    private static final byte[] COLOR_RED = new byte[]{(byte) 220, 38, 38};
    private static final byte[] COLOR_ORANGE = new byte[]{(byte) 234, (byte) 179, 8};
    private static final byte[] COLOR_YELLOW = new byte[]{(byte) 254, (byte) 240, (byte) 138};
    private static final byte[] COLOR_HEADER_BG = new byte[]{(byte) 241, (byte) 245, (byte) 249};

    public static String generateFilePath(String prefix) {
        String dir = getOutputDir();
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        return dir + "/" + prefix + "_" + timestamp + ".xlsx";
    }

    public static String getOutputDir() {
        File dir = new File(DEFAULT_OUTPUT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return DEFAULT_OUTPUT_DIR;
    }

    /**
     * Cree un workbook avec les styles predefinis.
     */
    public static XSSFWorkbook createWorkbook() {
        return new XSSFWorkbook();
    }

    /**
     * Cree un style pour les en-tetes de colonnes.
     */
    public static CellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(new XSSFColor(COLOR_GREEN, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(COLOR_HEADER_BG, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Cree un style pour les cellules de titre.
     */
    public static CellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(new XSSFColor(COLOR_GREEN, null));
        style.setFont(font);
        return style;
    }

    /**
     * Cree un style pour les cellules de sous-titre.
     */
    public static CellStyle createSubtitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    /**
     * Cree un style pour les montants.
     */
    public static CellStyle createMoneyStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00 \"EUR\""));
        return style;
    }

    /**
     * Cree un style pour les dates.
     */
    public static CellStyle createDateStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("dd/MM/yyyy"));
        return style;
    }

    /**
     * Cree un style pour les dates avec heure.
     */
    public static CellStyle createDateTimeStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("dd/MM/yyyy HH:mm"));
        return style;
    }

    /**
     * Cree un style avec fond rouge (pour alertes critiques).
     */
    public static CellStyle createRedBgStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(COLOR_RED, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Cree un style avec fond orange (pour alertes urgentes).
     */
    public static CellStyle createOrangeBgStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(COLOR_ORANGE, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Cree un style avec fond jaune (pour attention).
     */
    public static CellStyle createYellowBgStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(COLOR_YELLOW, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Cree une feuille de donnees avec en-tetes et lignes.
     *
     * @param wb        le workbook
     * @param sheetName le nom de la feuille
     * @param headers   les en-tetes
     * @param rows      les lignes de donnees
     * @return la feuille creee
     */
    public static Sheet createDataSheet(XSSFWorkbook wb, String sheetName,
                                         String[] headers, List<Object[]> rows) {
        Sheet sheet = wb.createSheet(sheetName);
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dateStyle = createDateStyle(wb);
        CellStyle dateTimeStyle = createDateTimeStyle(wb);
        CellStyle moneyStyle = createMoneyStyle(wb);

        // En-tetes
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Donnees
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            Object[] values = rows.get(r);
            for (int c = 0; c < values.length; c++) {
                Cell cell = row.createCell(c);
                setCellValue(cell, values[c], dateStyle, dateTimeStyle, moneyStyle);
            }
        }

        // Figer la premiere ligne + auto-filtre
        sheet.createFreezePane(0, 1);
        if (headers.length > 0 && !rows.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
        }

        // Auto-dimensionner les colonnes
        autoSizeColumns(sheet, headers.length);

        return sheet;
    }

    /**
     * Ajoute une feuille de couverture avec les infos pharmacie.
     */
    public static void addCoverSheet(XSSFWorkbook wb, String reportTitle,
                                       String pharmacyName, String date) {
        Sheet sheet = wb.createSheet("Couverture");
        CellStyle titleStyle = createTitleStyle(wb);
        CellStyle subtitleStyle = createSubtitleStyle(wb);

        Row row0 = sheet.createRow(1);
        Cell titleCell = row0.createCell(1);
        titleCell.setCellValue(pharmacyName);
        titleCell.setCellStyle(titleStyle);

        Row row2 = sheet.createRow(3);
        Cell reportCell = row2.createCell(1);
        reportCell.setCellValue(reportTitle);
        reportCell.setCellStyle(subtitleStyle);

        Row row4 = sheet.createRow(5);
        row4.createCell(1).setCellValue("Date de generation: " + date);

        Row row6 = sheet.createRow(7);
        row6.createCell(1).setCellValue("Genere par ApotiCare v1.0");

        sheet.setColumnWidth(1, 10000);

        // Placer cette feuille en premier
        wb.setSheetOrder("Couverture", 0);
    }

    /**
     * Definit la valeur d'une cellule selon le type de donnee.
     */
    private static void setCellValue(Cell cell, Object value,
                                       CellStyle dateStyle, CellStyle dateTimeStyle,
                                       CellStyle moneyStyle) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Float) {
            cell.setCellValue((Float) value);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
            cell.setCellStyle(moneyStyle);
        } else if (value instanceof LocalDate) {
            Date date = Date.from(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(date);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDateTime) {
            Date date = Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(date);
            cell.setCellStyle(dateTimeStyle);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value ? "Oui" : "Non");
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * Auto-dimensionne les colonnes.
     */
    public static void autoSizeColumns(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            try {
                sheet.autoSizeColumn(i);
                // Ajouter un peu de marge
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(currentWidth + 512, 15000));
            } catch (Exception e) {
                sheet.setColumnWidth(i, 4000);
            }
        }
    }

    /**
     * Sauvegarde le workbook dans un fichier.
     *
     * @param wb       le workbook
     * @param filePath le chemin du fichier
     * @return le chemin du fichier sauvegarde
     * @throws IOException si une erreur d'ecriture survient
     */
    public static String save(XSSFWorkbook wb, String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();

        logger.info("Export Excel genere: {}", filePath);
        return filePath;
    }
}
