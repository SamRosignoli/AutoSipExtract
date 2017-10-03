package br.com.nissan.infra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Excel {
	
	public static void main(String[] args) throws ParseException {
		
		File newFile = new File("C:\\Users\\xl02926\\Sip Extract\\APPLAUSO - 105.xls");
		
		Date date = DateUtils.parseDate("03/10/2017 14:30", "dd/MM/yyyy HH:mm");
		
		Excel e = new Excel();
		e.incluirColunaDataHora(date, newFile);
		
	}

	public Excel() {
	}

	public void incluirColunaDataHora(Date dtHrArquivo, File newFile) {

		try {
			
			HSSFWorkbook wk = new HSSFWorkbook(new FileInputStream(newFile));
			HSSFSheet ws = wk.getSheetAt(0);
			HSSFRow row = ws.getRow(0);
			HSSFCell c = null;

			int colNum = row.getLastCellNum();
			int rowNum = ws.getLastRowNum() + 1;
			int countRow = 1;
			System.out.println(colNum);
			System.out.println(rowNum);
			
			
			while (countRow <rowNum) {
				HSSFRow r = ws.getRow(countRow);
				c = r.getCell(colNum);
				if (c == null) {
					c = r.createCell(colNum);
				}
				c.setCellType(CellType.STRING);

				DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");

				c.setCellValue(df.format(dtHrArquivo));
				
				countRow = countRow + 1;
					
			}
			


			FileOutputStream out = new FileOutputStream(newFile);
			wk.write(out);
			wk.close();
			out.flush();
			out.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*public File gerarExcel() {

		XSSFWorkbook wb = new XSSFWorkbook();

		XSSFSheet sheet = wb.createSheet("Itens de Reemplazo");

		int rowCount = -1;
		int columnCount = -1;
		XSSFRow row = null;
		XSSFCell cell = null;

		*//*************** header ****************//*
		row = sheet.createRow(++rowCount);
		columnCount = -1;

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.STRING);
		cell.setCellValue("Referência");

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.STRING);
		cell.setCellValue("Descrição da Referência");

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.STRING);
		cell.setCellValue("Código de Reemplazo do Item");

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.STRING);
		cell.setCellValue("Descrição Única do Item c/ Reemplazo");

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.STRING);
		cell.setCellValue("Qtde Referencias");
		*//*************** header ****************//*

		*//*************** body ****************//*

		row = sheet.createRow(++rowCount);
		columnCount = -1;

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.STRING);
		cell.setCellValue("");

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.STRING);
		cell.setCellValue("");

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.STRING);
		cell.setCellValue("");

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.STRING);
		cell.setCellValue("");

		cell = row.createCell(++columnCount);
		cell.setCellType(CellType.NUMERIC);

		*//*************** body ****************//*

		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File(System.getProperty("user.home") + "/Desktop"));
		fc.addChoosableFileFilter(new FileNameExtensionFilter("Arquivos Excel (*.xlsx)", "xlsx"));
		fc.setAcceptAllFileFilterUsed(false);
		fc.setMultiSelectionEnabled(false);
		fc.setDialogTitle("Salvar arquivo final c/ Agrupamento de Itens de Reemplazo");
		int i = fc.showSaveDialog(null);
		while (i != JFileChooser.APPROVE_OPTION) {
			i = fc.showSaveDialog(null);
		}

		// File file = fc.getSelectedFile();
		String absolutePath = fc.getSelectedFile().getAbsolutePath();
		String ext = StringUtils.right(absolutePath, 5);
		if (!StringUtils.equalsIgnoreCase(ext, ".xlsx")) {
			absolutePath = absolutePath + ".xlsx";
		}

		File file = null;
		FileOutputStream os = null;

		try {

			file = new File(absolutePath);
			os = new FileOutputStream(file);

			wb.write(os);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Erro ao gravar o arquivo final Excel:\n" + e.getMessage(), "PAR.TAR - Agrupamento Itens de Reemplazo", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		} finally {
			try {
				wb.close();
				os.flush();
				os.close();
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}

		return file;

	}*/

}
