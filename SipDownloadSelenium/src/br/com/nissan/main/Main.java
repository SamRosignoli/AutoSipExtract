package br.com.nissan.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import br.com.nissan.domain.Concessionaria;
import br.com.nissan.domain.User;
import br.com.nissan.infra.Excel;

public class Main {

	// constantes
	private static final String propertiesDefaultName = "sip_download_config.properties";
	private static final String propertieCsvPath = "csv-path-download";
	private static final String propertieUser = "user";
	private static final String propertiePass = "pass";
	private static Logger logger = Logger.getLogger("SipLog");
	private static FileHandler fh = null;

	// vari�veis
	private static WebDriver driver = null;

	private static JavascriptExecutor js = null;

	private static String downloadFilepath;

	private static String csvPath;

	private static Properties properties;

	public static void main(String[] args) {

		confLogger();

		try {

			// escreve mensagens no log
			logger.info("INICIANDO EXTRACAO");

			// pega o tempo do sistema em nanosegundos
			long startTime = System.nanoTime();

			// Antes de qualquer outra coisa define o arquivo properties
			logger.info("Carregando o arquivo properties.");
			properties = getPropertiesConfig();
			logger.info("Properties OK!");

			// Verifica se o diret�rio j� existe, caso contr�rio cria um novo
			logger.info("Configurando Diret�rio de Download.");
			downloadFilepath = getDownloadFilepath();
			logger.info("Diret�rio de Download OK >>> " + downloadFilepath);

			// Define o diret�rio para salvar o CSV
			logger.info("Configurando Diret�rio do Arquivo CSV final.");
			csvPath = getCsvPath();
			logger.info("Diret�rio do Arquivo CSV final OK >>> " + csvPath);

			// Manipula os arquivos Excel com Apache POI
			Excel excel = new Excel(logger);

			// deleta todos os arquivos existentes na pasta sempre que executar uma nova rodada
			logger.info("Limpando Diret�rio de Downloads.");
			FileUtils.cleanDirectory(new File(downloadFilepath));
			logger.info("Limpeza OK!");

			logger.info("Carregando o Driver do Chrome.");
			String driverPath = getDriverPath();
			System.setProperty("webdriver.chrome.driver", driverPath);
			logger.info("Driver OK!");

			// abre o Chrome j� com as op��es configuradas (Ex.: maximizado)
			driver = new ChromeDriver(getChromeOptions());

			// possibilita a execu��o de javascript
			// faz todas as opera��es atrav�s de javascript por ser mais robusto que o m�todo driver.click()
			// o m�todo driver.click() s� funciona se estiver com a janela do browser ativa e com o elemento vis�vel
			js = (JavascriptExecutor) driver;

			// faz o login
			logger.info("Login no SIP.");
			login();
			logger.info("Login OK.");
			Thread.sleep(2000);

			// Itera��o em todas as concession�rias existentes no Select da p�gina para baixar o arquivo anal�tico
			// J� que no Selenium n�o � poss�vel acessar um WebElement depois de um refresh na p�gina em uma itera��o, guarda o Set de concession�rias antes para conseguir iterar depois.
			List<Concessionaria> set = optionsToDealerList();
			int ct = 0;
			for (Concessionaria conc : set) {
				// inicia o timer da concession�ria
				long startCons = System.nanoTime();

				String codDealer = conc.getCodigo();
				String descDealer = conc.getDescricao();

				// ignora se for a op��o '33 - Nissan F�brica' ou a op��o '1 - SIP Nissan'
				if (!StringUtils.equalsIgnoreCase(codDealer, "33") && !StringUtils.equalsIgnoreCase(codDealer, "1")) {

					if (ct++ > 0) {
						// para trocar de concession�ria tem de obrigatoriamente clicar na home do SIP antes
						driver.get("http://sipnissan.com.br/Sip/jsf_pages/home.jsf");
						Thread.sleep(2000);
					}

					// Seleciona a concession�ria e aguarda carregar
					js.executeScript("document.getElementById('formEmp:empresa').value = '" + codDealer + "';");
					js.executeScript("document.getElementById('formEmp:empresa').onchange();");
					Thread.sleep(2000);

					// Seleciona o usu�rio e extrai a Data/Hora da Carga do Arquivo
					// tenta at� o �ltimo usu�rio, se n�o houver, retorna nulo/vazio
					Date dtHrArquivo = getDataHoraCargaArquivo();
					Thread.sleep(1000);

					// Se n�o houve carga de arquivo, ignora e parte para o pr�ximo
					if (dtHrArquivo != null) {
						System.out.println("/n");
						logger.info("Extraindo o arquivo da concessionaria " + descDealer);
						String dtHrCarga = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(dtHrArquivo);
						logger.info("Data/Hora da Carga do Arquivo: " + dtHrCarga);

						clickPesquisar();

						// verifica se terminou a busca
						boolean pesquisaOk = waitPesquisar();
						if (!pesquisaOk) {
							// Log aqui da concession�ria que n�o conseguiu executar a pesquisa depois de 5mn (300seg)
							logger.warning("Nao foi possivel realizar a pesquisa para a concession�ria " + descDealer + " porque excedeu o tempo de 5mn para retornar resultado.");
							continue;
						}

						File xls = null;
						int count = 0;
						// vai tentar at� 10 vezes fazer o download, caso contrario sai do loop para n�o ficar eternamente
						while (xls == null && count < 10) {

							try {
								// clica para fazer o download
								js.executeScript("document.getElementById('formE:j_idt945').parentElement.click();");
							} catch (Exception e) {
								logger.info("Download ainda em andamento para a concession�ria " + descDealer);
								// se teve erro, ignora e espera mais 5 segundos
								// pode ocorrer de o download ainda estar em andamento
								// neste caso vai gerar erro em uma nova tentativa e por isso captura aqui
							} finally {
								Thread.sleep(5000);
							}

							// tenta renomear o excel depois do download se ach�-lo
							// retorna null se n�o encontr�-lo
							xls = renomeiaXls(descDealer);

							if (xls != null) {
								// Se achou � porque o download terminou com sucesso
								// Ent�o inclui a coluna com a Data/Hora de extra��o do Arquivo
								excel.incluirColunaDataHora(dtHrArquivo, xls);
							} else {
								// se for null pode ser o erro '500' no SIP ao tentar fazer download do arquivo
								// Neste caso, faz o navegador voltar e tenta o download de novo.
								boolean erro500 = isErro500();
								if (erro500) {
									logger.info("Erro 500 na concession�ria " + descDealer);
									driver.navigate().back();
									logger.info("Nova tentativa...");
								}
								Thread.sleep(3000);
							}
							count++;
						}

						boolean ok = (xls != null);
						//
						long stopCons = System.nanoTime();
						if (ok) {
							logger.info("Download " + descDealer + " ok!");
							// subtrai o tempo final extra�do do sistema do tempo inicial e divide por 1000000000 para dar a resposta em segundos
							logger.info("Tempo para download " + descDealer + ": " + (stopCons - startCons) / 1000000000 + " segundos.");
						} else {
							logger.warning("Download " + descDealer + " erro de timeout!");
						}

					}

					// verifica se houve carga do arquivo procurando pela data da carga. Quando a carga n�o feita
					// o campo de data fica vazio
					if (dtHrArquivo == null) {
						logger.warning("Download " + descDealer + " nao ocorreu por falta de carga do arquivo");
					}

				}

			}

			// Por fim, cria o arquivo final, copia o conte�do para ele, salva e fecha
			excel.gerarCsv(csvPath);
			logger.info("Arquivo final do SIP gerado com sucesso!");

			long stopTime = System.nanoTime();
			logger.info("Tempo total do processo: " + (((stopTime - startTime) / 1000000000) / 60) + " minutos.");
			fh.close();
			DateFormat dfLog = new SimpleDateFormat("yyyyMMdd_HHmm");
			new File(System.getProperty("user.home") + "\\log.log").renameTo(new File(csvPath + "\\SIP_" + dfLog.format(Calendar.getInstance().getTime()) + ".log"));

		} catch (Exception e) {
			// JOptionPane.showMessageDialog(null, "Erro Indeterminado: " + e.getMessage(), tituloMessage, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

		} finally {
			// fecha as conex�es com o driver
			if (driver != null) {
				driver.close();
				driver.quit();
				driver = null;
				System.gc();
				System.exit(0);
			}

		}

	}

	/**
	 * Clica em pesquisar
	 */
	private static void clickPesquisar() {
		js.executeScript("document.getElementById('formE:modelButton').getElementsByTagName('a')[3].click();");
	}

	private static void confLogger() {

		try {
			// Configura o logger com handler e formatter
			fh = new FileHandler(System.getProperty("user.home") + "\\log.log");
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*
		 * Tipos de log logger.severe("EXEMPLO DE ERRO SEVERO"); logger.warning("EXEMPLO DE AVISO"); logger.info("EXEMPLO DE INFO");
		 */
	}

	/**
	 * Gera, se ainda n�o existir, o arquivo 'sip_download_config.properties' no mesmo diret�rio do projeto final (jar execut�vel) j� com os properties padr�es configurados.
	 * 
	 * @return
	 * @throws Exception
	 */
	private static Properties getPropertiesConfig() throws Exception {

		String projectDir = System.getProperty("user.dir");

		int lastIndexOf = projectDir.lastIndexOf("\\");
		String diretorio = StringUtils.substring(projectDir, 0, lastIndexOf);
		// cria o diretorio se ainda n�o existir
		File f = new File(diretorio);
		if (!f.exists()) {
			String msg = "N�o foi poss�vel encontrar o diret�rio '" + diretorio + "' para gerar o arquivo '" + propertiesDefaultName + "'.";
			logger.severe(msg);
			throw new Exception(msg);
		}

		String propsPath = diretorio + File.separator + propertiesDefaultName;
		File propsFile = new File(propsPath);
		if (!propsFile.exists()) {
			try {
				logger.info("Arquivo properties inexistente, criando arquivo padr�o.");
				propsFile.createNewFile();
				writeDefaultProperties(propsPath);
				logger.info("Arquivo de properties padr�o criado com sucesso");
			} catch (Exception e) {
				String msg = "N�o foi poss�vel criar o arquivo " + propertiesDefaultName + "' >>> " + e.getMessage();
				logger.severe(msg);
				throw new Exception(msg);
			}
		}

		Properties prop = loadProperties(propsFile);

		return prop;

	}

	/**
	 * Carrega o arquivo Properties.
	 * 
	 * @param propsFile
	 * @return
	 * @throws Exception
	 */
	private static Properties loadProperties(File propsFile) throws Exception {

		Properties prop = new Properties();

		FileInputStream in = null;

		try {

			// carrega o properties
			in = new FileInputStream(propsFile);
			prop.load(in);

			Set<Object> keySet = prop.keySet();
			logger.info("Arquivo '" + propertiesDefaultName + "' carregado com sucesso:");
			for (Object obj : keySet) {
				logger.info(obj + " = " + prop.getProperty((String) obj));
			}

		} catch (Exception e) {
			String msg = "Erro ao carregar o arquivo de configura��o '" + propertiesDefaultName + "' >>> " + e.getMessage();
			logger.severe(msg);
			throw new Exception(msg);

		} finally {
			try {
				in.close();
			} catch (Exception e) {
				// ignore
			}
		}

		return prop;
	}

	/**
	 * Escreve um Properties padr�o.
	 * 
	 * @param path
	 * @throws Exception
	 */
	private static void writeDefaultProperties(String path) throws Exception {

		Properties p = new Properties();

		OutputStream out = new FileOutputStream(path);

		String defaultCsvPath = getDefaultCsvPath();
		defaultCsvPath = StringUtils.replace(defaultCsvPath, "\\", "/");

		p.setProperty(propertieUser, "srodrigues");
		p.setProperty(propertiePass, "a1");
		p.setProperty(propertieCsvPath, defaultCsvPath);

		StringBuilder comentarios = new StringBuilder();
		comentarios.append("O diret�rio padr�o para salavar o CSV (" + propertieCsvPath + ") deve ser informado sempre com a \"barra para frente\" >> '/'.");
		comentarios.append(StringUtils.CR + StringUtils.LF);
		comentarios.append("Ex.: 'D:/LocalData/x888541/Documents'.");

		p.store(out, comentarios.toString());

		if (out != null) {
			out.close();
		}

	}

	/**
	 * Define qual vai ser o diret�rio para salvar o arquivo CSV final.<br>
	 * Primeiro tenta pegar do arquivo de configura��o '.properties'.<br>
	 * Caso contr�rio salva na �rea de trabalho diret�rio padr�o do usu�rio.
	 * 
	 * @return
	 * @throws Exception
	 */
	private static String getCsvPath() throws Exception {

		String propertyCsv = properties.getProperty(propertieCsvPath);

		String csvDir = StringUtils.isNoneEmpty(propertyCsv) ? propertyCsv : getDefaultCsvPath();

		// cria o diret�rio se n�o existir
		File dir = new File(csvDir);
		if (!dir.exists()) {
			try {
				dir.mkdirs();
			} catch (Exception ex) {
				String msg = "N�o foi poss�vel criar o diret�rio para gerar o CSV dos arquivos SIP >>> " + ex.getMessage();
				logger.severe(msg);
				throw new Exception(msg);
			}
		}

		String csvPath = dir.getAbsolutePath();

		return csvPath;
	}

	private static String getDefaultCsvPath() {
		return System.getProperty("user.home") + "\\Sip Csv Final";
	}

	/**
	 * Verica se houve erro 500, e retorna true caso ocorra, a main solicitar� que o driver volte uma p�gina e tente novamente
	 * 
	 * @return
	 */
	private static boolean isErro500() {

		boolean erro500 = false;

		try {

			Object obj500 = js.executeScript("return document.getElementsByTagName('h1')[0].innerText;");

			String txt500 = (obj500 != null && obj500 instanceof String) ? StringUtils.left(StringUtils.trim((String) obj500), 15) : "";

			erro500 = StringUtils.equalsIgnoreCase(txt500, "HTTP Status 500");

		} catch (Exception e) {
			// ignore
			erro500 = false;
		}

		return erro500;

	}

	/**
	 * Depois que clica em pesquisar, verifica se terminou a busca olhando se a TD 'Data da Pesquisa' foi preenchida.<br>
	 * <br>
	 * 
	 * Tamb�m, para evitar que entre em um 'Loop Eterno', tenta clicar novamente em Pesquisar depois de um certo tempo (a cada 1,20mn - 80 segundos).<br>
	 * <br>
	 * 
	 * Caso fique mais de 5mn parado no loop, passa para o pr�ximo dealer!
	 * 
	 */
	private static boolean waitPesquisar() {
		Object jsReturn = null;
		String dtPesquisa = null;
		int ctToClick = 0;
		boolean isEmpty = true;
		while (isEmpty) {

			try {

				jsReturn = js.executeScript("return document.getElementById('formE:planejamento_content').getElementsByTagName('td')[3].innerText;");
				dtPesquisa = (jsReturn != null && jsReturn instanceof String) ? StringUtils.trim((String) jsReturn) : "";
				Thread.sleep(1000);

				ctToClick++;
				if (ctToClick == 80 || ctToClick == 160 || ctToClick == 240) {
					clickPesquisar();
					Thread.sleep(1000);
				}

				if (ctToClick == 300) {
					return false;
				}

				isEmpty = StringUtils.isEmpty(dtPesquisa);

			} catch (Exception e) {
				// ignore
			}

		}

		return !isEmpty;

	}

	/**
	 * Renomeia o arquivo com o nome da concessionaria e apaga o antigo, come�ado por "DWAna". <br>
	 * Tamb�m garante que o programa n�o deixar� arquivos duplicados, terminados em ").xls"
	 * 
	 * @param descDealer
	 * @param dtHrArquivo
	 * @return
	 */
	private static File renomeiaXls(String descDealer) {
		File folder = new File(downloadFilepath);
		File[] listOfFiles = folder.listFiles();
		for (File f : listOfFiles) {
			if (f.isFile()) {
				String fName = f.getName();

				// garante que n�o vai pegar arquivos que porventura tenham sido salvos 2x
				// quando isso ocorre, o final deles fica diferente: '_Gerar (1).xls'
				boolean checkIni = "DWAna".equalsIgnoreCase(StringUtils.left(fName, 5));
				boolean checkExtension = "_Gerar.xls".equalsIgnoreCase(StringUtils.right(fName, 10));

				if (checkIni && checkExtension) {
					File oldFile = new File(downloadFilepath + "\\" + fName);
					File newFile = new File(downloadFilepath + "\\" + descDealer + ".xls");
					boolean renameToOk = oldFile.renameTo(newFile);
					oldFile.delete();
					return renameToOk ? newFile : null;
				}

				boolean checkDuplicado = ").xls".equalsIgnoreCase(StringUtils.right(fName, 5));
				if (checkDuplicado) {
					f.delete();
				}

			}
		}
		return null;
	}

	/**
	 * Salva todas as op��es do combo de Dealers em um List para possibilitar a itera��o em cada option depois.<br>
	 * 
	 * N�o permite repetidos / Usa List para garantir a ordem dos itens na lista.
	 * 
	 * @return HashSet com os objetos Concessionaria
	 */
	@SuppressWarnings("unchecked")
	private static ArrayList<Concessionaria> optionsToDealerList() {

		ArrayList<Concessionaria> list = new ArrayList<Concessionaria>();

		Object jsReturn = js.executeScript("return document.getElementById('formEmp:empresa').getElementsByTagName('option');");

		int ct = 0;
		List<WebElement> jsReturnList = (List<WebElement>) jsReturn;
		for (WebElement option : jsReturnList) {

			String codigo = StringUtils.trim(option.getAttribute("value"));
			String descricao = StringUtils.trim(option.getText());
			int index = ct++;

			Concessionaria c = new Concessionaria(codigo, descricao, index);
			if (!list.contains(c)) {
				list.add(c);
			}

		}

		return list;
	}

	/**
	 * <b>Define as op��es para abertura do browser.</b><br>
	 * Ex.:<br>
	 * -Abrir j� maximizado<br>
	 * -Diret�rio padr�o para downloads
	 * 
	 * @return org.openqa.selenium.chrome.ChromeOptions
	 * @throws Exception
	 */
	private static ChromeOptions getChromeOptions() throws Exception {

		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("--start-maximized");

		HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
		chromePrefs.put("profile.default_content_settings.popups", 0);
		chromePrefs.put("download.default_directory", downloadFilepath);
		chromeOptions.setExperimentalOption("prefs", chromePrefs);
		DesiredCapabilities cap = DesiredCapabilities.chrome();
		cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
		cap.setCapability(ChromeOptions.CAPABILITY, chromeOptions);

		return chromeOptions;

	}

	/**
	 * Verifica se o diret�rio j� existe, caso contr�rio cria um novo na pasta raiz do usu�riuo.
	 * 
	 * Ex.: D:\LocalData\x888541\Sip Extract
	 * 
	 * @return String com o path do diret�rio criado/j� existente
	 * @throws Exception
	 *             lan�a uma Exception caso n�o consiga criar o diret�rio no SO.
	 */
	private static String getDownloadFilepath() throws Exception {

		String userHome = System.getProperty("user.home");

		File theDir = new File(userHome + "\\Sip Extract");

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			try {
				theDir.mkdirs();
			} catch (Exception ex) {
				String msg = "N�o foi poss�vel criar o diret�rio para extra��o dos arquivos SIP >>> " + ex.getMessage();
				logger.severe(msg);
				throw new Exception(msg);
			}

		}

		String absolutePath = theDir.getAbsolutePath();

		return absolutePath;

	}

	/**
	 * Pega a Data/Hora da Carga do Arquivo iterando por cada um dos usu�rios existentes para a concession�ria em quest�o.<br>
	 * Se achar em qualquer um deles j� retornar, n�o vai at� o fim. Se n�o teve carga para nenhum dos usu�rios, ent�o retorna null.
	 * 
	 * @param conc
	 * 
	 * @param driver
	 * @return Date
	 * @throws InterruptedException
	 * @throws ParseException
	 */
	private static Date getDataHoraCargaArquivo() throws InterruptedException, ParseException {

		List<User> users = optionsToUserList();

		int ct = 0;
		for (User u : users) {

			String codigo = u.getCodigo();

			// ignora a op��o '0'
			if (!StringUtils.equalsIgnoreCase(codigo, "0")) {

				if (ct > 0) {
					// para trocar de usu�rio tem de obrigatoriamente clicar na home do SIP antes
					// driver.findElement(By.id("j_idt29:j_idt30")).click();
					js.executeScript("document.getElementById('j_idt29:j_idt30').click();");
					Thread.sleep(2000);
				}

				// seleciona o usu�rio
				js.executeScript("document.getElementById('formEmp:usuario').value = '" + codigo + "';");
				js.executeScript("document.getElementById('formEmp:usuario').onchange();");
				Thread.sleep(2000);

				// Tenta achar a data e se achar j� retorna, n�o vai para o pr�ximo
				Date dataHoraArquivo = tryToGetDataHoraByUser();
				if (dataHoraArquivo != null) {
					return dataHoraArquivo;
				}

				ct++;

			}

		}

		return null;

	}

	/**
	 * Salva todas as op��es do combo de Users em um List para possibilitar a itera��o em cada option depois.<br>
	 * 
	 * N�o permite repetidos / Usa List para garantir a ordem dos itens na lista.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static List<User> optionsToUserList() {

		ArrayList<User> list = new ArrayList<User>();

		Object jsReturn = js.executeScript("return document.getElementById('formEmp:usuario').getElementsByTagName('option');");

		int ct = 0;
		List<WebElement> jsReturnList = (List<WebElement>) jsReturn;
		for (WebElement opt : jsReturnList) {

			String codigo = StringUtils.trim(opt.getAttribute("value"));
			String nome = StringUtils.trim(opt.getText());
			int index = ct++;

			User u = new User(codigo, nome, index);
			if (!list.contains(u)) {
				list.add(u);
			}

		}

		return list;

	}

	/**
	 * Pega a Data/Hora da Carga do Arquivo considerando o usu�rio atualmente selecionado. Se n�o teve carga para o usu�rio selecionado retorna null.
	 * 
	 * @param driver
	 * @param optU
	 * @return
	 * @throws InterruptedException
	 * @throws ParseException
	 */
	private static Date tryToGetDataHoraByUser() throws InterruptedException, ParseException {

		// Acessa o Anal�tico e aguarda carregar
		driver.get("http://sipnissan.com.br/Sip/jsf_pages/automobilistico/autAnalitico/autAnalitico.jsf?apenasPesquisa=false");
		Thread.sleep(3000);

		// tenta ler a data na <td> que contem ela
		Object jsReturn = js.executeScript("return document.getElementById('formE:planejamento_content').getElementsByTagName('td')[1].innerText;");

		String dtHrStr = (jsReturn != null && jsReturn instanceof String) ? StringUtils.trim((String) jsReturn) : "";

		Date parseDate = null;
		if (StringUtils.isNotEmpty(dtHrStr)) {
			parseDate = DateUtils.parseDate(dtHrStr, "dd/MM/yyyy HH:mm");
		}

		return parseDate;
	}

	/**
	 * Pega o driver diretamente dos resources do projeto e extrai o mesmo na pasta raiz do usu�rio no SO em quest�o.<br>
	 * 
	 * Ex.: 'C:\Users\Sidney Rodrigues\ChromeDriver\chromedriver.exe'
	 * 
	 * @return
	 * @throws Exception
	 */
	private static String getDriverPath() throws Exception {

		String userHome = System.getProperty("user.home");

		String diretorio = userHome + File.separator + "ChromeDriver";

		// cria o diretorio se ainda n�o existir
		File f = new File(diretorio);
		if (!f.exists()) {
			try {
				f.mkdirs();
			} catch (Exception ex) {
				String msg = "N�o foi poss�vel criar o diret�rio ChromeDriver no user.home >>> " + ex.getMessage();
				logger.severe(msg);
				throw new Exception(msg);
			}
		}

		// copia o driver se ainda n�o existir
		File chromeDriver = new File(diretorio + File.separator + "chromedriver.exe");
		if (!chromeDriver.exists()) {

			chromeDriver.createNewFile();

			ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			URL resource = classLoader.getResource("chromedriver.exe");

			org.apache.commons.io.FileUtils.copyURLToFile(resource, chromeDriver);

		}

		String driverPath = chromeDriver.getAbsolutePath();

		return driverPath;

	}

	/**
	 * Faz o Login no SIP
	 * 
	 * @param driver
	 * @throws InterruptedException
	 */
	private static void login() throws Exception {

		String url = "http://sipnissan.com.br/Sip/login.jsf";

		String user = properties.getProperty(propertieUser);
		String pass = properties.getProperty(propertiePass);

		if (StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
			String msg = "Erro: Usu�rio e/ou Senha para login no SIP n�o foi informado no arquivo '" + propertiesDefaultName + "'.";
			logger.severe(msg);
			throw new Exception(msg);
		}

		driver.get(url);
		Thread.sleep(2000);

		js.executeScript("document.getElementById('j_idt11:Login').value = '" + user + "';");
		js.executeScript("document.getElementById('j_idt11:Senha').value = '" + pass + "';");
		js.executeScript("document.getElementById('j_idt11:j_idt19').click();");

	}

}
