package br.com.nissan.infra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;

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

	private StringBuilder sb;
	
	private PrintWriter pw;
	
	private boolean header = false;
	
	private static String csvPath;

	// ParseException
	// FileNotFoundException
	public static void main(String[] args) throws ParseException {

		File newFile = new File("C:\\Users\\xl02926\\Sip Extract\\APPLAUSO - 105.xls");

		Date date = DateUtils.parseDate("03/10/2017 14:30", "dd/MM/yyyy HH:mm");
		
		Excel e = new Excel();
		e.incluirColunaDataHora(date, newFile);
		
		e.gerarCsv(csvPath);

	}

	public Excel() {
		sb = new StringBuilder();
		generateHeaderCsv();
	}

	private void generateHeaderCsv() {
		pw = null;
		try {
			pw = new PrintWriter(new File("D://test.csv"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	public void incluirColunaDataHora(Date dtHrArquivo, File file) {

		HSSFWorkbook wk = null;
		HSSFSheet ws = null;
		HSSFRow row = null;
		HSSFCell cellDtHr = null;
		HSSFCell cellBloq = null;
		HSSFCell cellBloqCheck = null;

		FileOutputStream out = null;

		try {

			wk = new HSSFWorkbook(new FileInputStream(file));
			ws = wk.getSheetAt(0);
			row = ws.getRow(0);

			int colNum = row.getLastCellNum();
			int colBloq = colNum + 1;
			int colCheckBloq = 4; // coluna E
			int rowNum = ws.getLastRowNum() + 1;
			int countRow = 1;
			int countCol = 0;

			
			if (header == false) {
				while (countCol < colNum) {
				
					HSSFRow hRow = ws.getRow(0);
					HSSFCell cell = hRow.getCell(countCol);
					String hContent = cell.getStringCellValue();
					System.out.println(hContent);
					sb.append(hContent);
					sb.append(";");
					if (countCol + 1 == colNum) {
						sb.append("Data e hora da carga");
						sb.append(";");
						sb.append("Bloqueado");
						sb.append(";");
						sb.append("\n");
					}
					countCol = countCol + 1;
				}
				countCol = 0;
				header = true;
			}
			while (countRow < rowNum) {

				HSSFRow r = ws.getRow(countRow);

				// Data/Hora
				cellDtHr = r.getCell(colNum);
				if (cellDtHr == null) {
					cellDtHr = r.createCell(colNum);
				}
				cellDtHr.setCellType(CellType.STRING);

				DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");

				cellDtHr.setCellValue(df.format(dtHrArquivo));

				// check se tem bloqueio
				cellBloqCheck = r.getCell(colCheckBloq);
				String color = cellBloqCheck != null ? cellBloqCheck.getCellStyle().getFillForegroundColorColor().getHexString() : "";
				boolean temBloq = color != null ? !color.equalsIgnoreCase("0:0:0") : false;

				// Bloqueios
				cellBloq = r.getCell(colBloq);
				if (cellBloq == null) {
					cellBloq = r.createCell(colBloq);
				}
				cellBloq.setCellType(CellType.STRING);
				cellBloq.setCellValue(temBloq ? "SIM" : "NÃO");

				while (countCol < colNum + 2) {
					
					HSSFRow bRow = ws.getRow(countRow);
					HSSFCell cell = bRow.getCell(countCol);
					cell.setCellType(CellType.STRING);
					String hContent = cell.getStringCellValue();
					System.out.println(hContent);
					sb.append(hContent);
					sb.append(";");
					if (countCol + 1 == colNum + 2) {
						sb.append("\n");
					}
					countCol = countCol + 1;
				}
				countCol = 0;
				
				countRow = countRow + 1;

			}
		

			out = new FileOutputStream(file);
			wk.write(out);

		} catch (Exception e) {
			e.printStackTrace();

		} finally {

			try {
				wk.close();
				out.flush();
				out.close();
			} catch (Exception e) {
			}

		}

	}

	public void gerarCsv(String path) {
		// Gerar arquivo CSV
			pw.write(sb.toString());
			System.out.println("done!");
			pw.flush();
			pw.close();
	}

	/**
	 * Gera um arquivo 'xlsx'. Por isso tem de usar XSSF
	 */
	public File gerarArquivoUnico() {

		// TODO - gerar arquivo único depois que extrair tudo

		XSSFWorkbook wb = new XSSFWorkbook();

		XSSFSheet sheet = wb.createSheet("Itens de Reemplazo");

		int rowCount = -1;
		int columnCount = -1;
		XSSFRow row = null;
		XSSFCell cell = null;

		/*************** header ****************/
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
		/*************** header ****************/

		/*************** body ****************/
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
		/*************** body ****************/

		File file = null;
		FileOutputStream os = null;

		try {

			file = new File("");
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

	}

}
