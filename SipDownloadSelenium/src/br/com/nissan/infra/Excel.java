package br.com.nissan.infra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.CharUtils;
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

	private StringBuilder sb;

	private boolean header = false;

	private final String crLf = Character.toString((char) 13) + Character.toString((char) 10);
	
	private String biFile = "D:\\LocalData\\xl02926\\ff_estoque_material_varejo.csv";
	
	File biFil = new File(biFile);

	/**
	 * método main para testes
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		

		String csvPath = "D:\\LocalData\\xl02926\\Documents";
		

		File newFile = new File("C:\\Users\\xl02926\\Sip Extract\\APJ JAPAN - 26.xls");

		Date date = DateUtils.parseDate("03/10/2017 14:30", "dd/MM/yyyy HH:mm");

		Excel e = new Excel();
		e.incluirColunaDataHora(date, newFile);

		e.gerarCsv(csvPath);

		String teste = null;
		@SuppressWarnings("unused")
		String retorno = e.trataString(teste);

	}

	/**
	 * aponta sb como uma nova StringBuilder
	 */
	public Excel() {
		sb = new StringBuilder();
	}

	/**
	 * Inclui uma coluna (AV) com data e hora da carga no SIP e uma coluna (AW) indicando bloqueio.<br>
	 * Logo após copia o conteúdo do arquivo para uma string e ao final do download de todos os arquivos,<br>
	 * copia o conteúdo todo para o arquivo CSV.
	 * 
	 * @param dtHrArquivo
	 * @param file
	 */
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

			// pega o numero da ultima coluna com valores na tabela. Obs: Índice começa com 0
			int colNum = row.getLastCellNum();
			int colBloq = colNum + 1;
			int colCheckBloq = 4; // coluna E
			// pega o numero da ultima linha com valores na tabela e adiciona 1. Obs: Índice começa com 0
			int rowNum = ws.getLastRowNum() + 1;
			int countRow = 1;
			int countCol = 0;

			// Verifica se o cabeçalho ainda não foi feito. Caso positivo, copia a primeira linha do primeiro arquivo baixado,
			// adiciona as colunas de data/hora e bloqueio e sinaliza o cabeçalho como feito
			if (header == false) {
				while (countCol < colNum) {

					HSSFRow hRow = ws.getRow(0);
					HSSFCell cell = hRow.getCell(countCol);
					String hContent = cell.getStringCellValue();
					sb.append(trataString(hContent));
					sb.append(";");
					if (countCol + 1 == colNum) {
						sb.append("Data e hora da carga");
						sb.append(";");
						sb.append("Bloqueado");
						sb.append(crLf);
					}
					countCol = countCol + 1;
				}
				countCol = 0;
				header = true;
			}

			/*
			 * Contador vertical: enquanto o número da linha atual for menor que o número total de linhas, executa o contador horizontal. Contador horizontal: a cada passagem do contador vertical, realiza um
			 * while que copia o conteúdo de cada célula até que o número do contador seja igual ao número de colunas + 2 (data/hora e bloqueio). No final, quebra a linha e começa a copiar a próxima, adicionando
			 * 1 no cont
			 */

			// CONTADOR VERTICAL
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

				// CONTADOR HORIZONTAL
				while (countCol < colNum + 2) {

					HSSFRow bRow = ws.getRow(countRow);
					HSSFCell cell = bRow.getCell(countCol);
					cell.setCellType(CellType.STRING);
					String hContent = cell.getStringCellValue();
					sb.append(trataString(hContent));
					if (countCol + 1 == colNum + 2) {
						sb.append(crLf);
					} else {
						sb.append(";");
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

	/**
	 * Trata a String, retira caracteres de controle da tabela ASCII (0 A 31 E 127) EX: Alt+3 (End of Text)
	 * 
	 * @param str
	 * @return
	 */
	private String trataString(String str) {
		String retorno = str;
		int length = str != null ? str.length() : 0;
		for (int i = 0; i < length; i++) {

			char charAt = str.charAt(i);
			char indexString = str != null ? charAt : 'a';
			// Boolean para verificar se o caractere selecionado é um caracter de controle ASCII
			boolean c = CharUtils.isAsciiControl(indexString);
			//
			boolean c2 = indexString == ';' || indexString == '"' ? true : false;
			//Verificação c=true ou c2=true
			if (c || c2) {
				retorno = StringUtils.replace(retorno, ("" + charAt), "");
			}

		}
		// trim para impedir espaços indesejados nas células
		retorno = StringUtils.trim(retorno);
		return retorno;
	}

	/**
	 * Gera o arquivo CSV, copia todos os dados armazenados na StringBuilder sb, salva o arquivo e fecha no local indicado (path)
	 * 
	 * @param path
	 * @throws Exception
	 */
	public void gerarCsv(String path) throws Exception {

		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmm");
		path = path + "\\SIP_" + df.format(Calendar.getInstance().getTime()) + ".csv";

		PrintWriter pw = null;
		PrintWriter pwBI = null;

		try {

			File file = new File(path);
			if(!file.exists()) {
				file.createNewFile();
			}
			
			// Força para salvar em ISO-8859-1
			//Arquivo local
			pw = new PrintWriter(file, "ISO-8859-1");
			//Arquivo para o BI (caminho da pasta ainda sujeito a alteração)
			pwBI = new PrintWriter(biFile,"ISO-8859-1");
			
			pw.write(sb.toString());
			pwBI.write(sb.toString());
			
			System.out.println("Terminado!");

		} catch (Exception e) {
			throw new Exception("Erro ao gerar o arquivo CSV Final >>> " + e.getMessage());

		} finally {
			try {
				if (pw != null && pwBI !=null) {
					pw.flush();
					pw.close();
					
					pwBI.flush();
					pwBI.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}

	}

	// CÓDIGO ABAIXO NÃO UTILIZADO, SOMENTE EXEMPLO

	/**
	 * Gera um arquivo 'xlsx'. Por isso deve-se usar XSSF
	 */
	public File gerarArquivoUnico() {

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
