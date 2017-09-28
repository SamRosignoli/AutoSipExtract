package br.com.nissan.infra;

import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Excel {

	public Excel() {
	}

	/*public File gerarExcel(ItemList itens) {

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
		cell.setCellValue((String) "Referência");

		cell = row.createCell(++columnCount);
		cell.setCellValue((String) "Descrição da Referência");

		cell = row.createCell(++columnCount);
		cell.setCellValue((String) "Código de Reemplazo do Item");

		cell = row.createCell(++columnCount);
		cell.setCellValue((String) "Descrição Única do Item c/ Reemplazo");

		cell = row.createCell(++columnCount);
		cell.setCellValue((String) "Qtde Referencias");
		*//*************** header ****************//*

		*//*************** body ****************//*
		for (Item it : itens.iterator()) {

			int qtdeReemplazo = it.getQtdeReferencias();
			String codReemplazo = qtdeReemplazo > 1 ? it.getCodigoReemplazo() : "";
			String descReemplazo = qtdeReemplazo > 1 ? it.getDescricao() : "";

			for (Referencia r : it.iterator()) {
				row = sheet.createRow(++rowCount);
				columnCount = -1;

				cell = row.createCell(++columnCount);
				cell.setCellType(CellType.STRING);
				cell.setCellValue(r.getCodigo());

				cell = row.createCell(++columnCount);
				cell.setCellType(CellType.STRING);
				cell.setCellValue(r.getDescricao());

				cell = row.createCell(++columnCount);
				cell.setCellType(CellType.STRING);
				cell.setCellValue(codReemplazo);

				cell = row.createCell(++columnCount);
				cell.setCellType(CellType.STRING);
				cell.setCellValue(descReemplazo);

				cell = row.createCell(++columnCount);
				cell.setCellType(CellType.NUMERIC);
				
				if(qtdeReemplazo > 1 ) {
					cell.setCellValue(qtdeReemplazo);
				} else {
					cell.setCellValue("");
				}
				
			}

		}
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
