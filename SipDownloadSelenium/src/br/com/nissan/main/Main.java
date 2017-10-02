package br.com.nissan.main;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class Main {

	private static HashMap<String, Date> cargas;

	private static String downloadFilepath;

	public static void main(String[] args) {

		cargas = new HashMap<>();
		cargas.clear();
		WebDriver driver = null;

		String tituloMessage = "Selenium SIP Download";
		String codDealer = "";
		String descDealer = "";

		try {

			// Verifica se o diretório já existe, caso contrário cria um novo
			downloadFilepath = checkDir();

			// deletar todos os arquivos existentes na pasta
			deleteActualfiles();

			String driverPath = getDriver();
			System.setProperty("webdriver.chrome.driver", driverPath);

			// abre o Chrome já com as opções configuradas (Ex.: maximizado)
			driver = new ChromeDriver(getChromeOptions());

			// faz o login
			login(driver);
			Thread.sleep(5000);

			// Iteração em todas as concessionárias existentes no Select da página para
			// baixar o arquivo analítico
			// Ignora a opção 33 - Nissan
			// Ignora a opção 1 - SIP Nissan
			// Somente exporta os dados das concessionárias
			WebElement comboDealers = driver.findElement(By.id("formEmp:empresa"));
			List<WebElement> list = comboDealers.findElements(By.tagName("option"));
			for (WebElement optC : list) {

				codDealer = optC.getAttribute("value");
				descDealer = optC.getText();

				// -----------
				if (!"105".equalsIgnoreCase(codDealer)) {
					continue;
				}

				// ignora se for Nissan
				if (!StringUtils.equalsIgnoreCase(codDealer, "33") && !StringUtils.equalsIgnoreCase(codDealer, "1")) {
					// Seleciona a concessionária e aguarda carregar
					optC.click();
					Thread.sleep(3000);

					// Seleciona o usuário e Pega a Data/Hora da Carga do Arquivo
					// vai tentando até o último usuário, se não tiver retorna nulo/vazio
					Date dtHrArquivo = getDataHoraCargaArquivo(driver);
					Thread.sleep(3000);

					// Se não teve carga de arquivo, ignora e parte para o próximo
					if (dtHrArquivo != null) {

						String fileStr = descDealer + ".xls";
						System.out.println(fileStr);

						cargas.put(codDealer, dtHrArquivo);

						// clica em pesquisar
						WebElement pesquisar = driver.findElement(By.id("formE:modelButton")).findElements(By.tagName("a")).get(3);
						pesquisar.click();
						Thread.sleep(10000);

						//
						WebElement ScrollDetalhe = driver.findElement(By.id("formE:vlrDetalhe_toggler"));
						((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", ScrollDetalhe);
						Thread.sleep(500);

						WebElement aDetalhe = driver.findElement(By.id("formE:vlrDetalhe_toggler"));
						aDetalhe.findElement(By.tagName("span")).click();
						Thread.sleep(500);

						WebElement ScrollFilial = driver.findElement(By.id("formE:filial_toggler"));
						((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", ScrollFilial);
						Thread.sleep(1000);

						WebElement imgSave = driver.findElement(By.id("formE:j_idt945"));
						imgSave.click();
						Thread.sleep(3000);


						// new File("D:\\LocalData\\xl02926\\Downloads\\DWAna0002601450_Gerar.xls").renameTo(new File("Z:\\Relatório de Cobertura\\AutoSipExtract\\Extraction\\" + descDealer + ".xls"));

						File folder = new File(downloadFilepath);
						File[] listOfFiles = folder.listFiles();
						for (File f : listOfFiles) {
							if (f.isFile()) {
								
								String fName = f.getName();
								System.out.println("File " + fName);
								
								if("DWA".equalsIgnoreCase(StringUtils.left(fName, 3))) {
									File oldFile = new File(downloadFilepath + "\\" +fName);
									File newFile = new File(downloadFilepath + "\\" + descDealer + ".xls");
									oldFile.renameTo(newFile);
									//usar newFile com poi
									oldFile.delete();
									// alterar o nome
									// incluir coluna na direita
									// salvar
								}
								
								
							}
						}

					}

				}

			}

			JOptionPane.showMessageDialog(null, "Arquivo final do SIP gerado com sucesso!", tituloMessage, JOptionPane.INFORMATION_MESSAGE);

		} catch (TimeoutException e) {
			JOptionPane.showMessageDialog(null, "Erro de tempo de espera excedido: " + e.getMessage(), tituloMessage, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

		} catch (NoSuchElementException e) {
			JOptionPane.showMessageDialog(null, "Erro ao tentar encontrar um elemento na página do SIP", tituloMessage, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

		} catch (ParseException e) {
			JOptionPane.showMessageDialog(null, "Erro ao tentar ler a Data/Hora de carga do arquivo no dealer " + codDealer + " - " + descDealer, tituloMessage, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Erro Indeterminado: " + e.getMessage(), tituloMessage, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

		} finally {

			if (driver != null) {
				driver.close();
				driver.quit();
				driver = null;
				System.gc();
				System.exit(0);
			}

		}

	}

	private static void deleteActualfiles() {
		// TODO - deletar todos os arquivos existentes na pasta

	}

	/**
	 * Opções para abertura do browser. Ex.: abrir já maximizado
	 * 
	 * @return
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
	 * Verifica se o diretório já existe, caso contrário cria um novo
	 * 
	 * @return
	 * @throws Exception
	 */
	private static String checkDir() throws Exception {

		String downloadFilepath = System.getProperty("user.home");

		File theDir = new File(downloadFilepath + "\\Sip Extract");

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			try {
				theDir.mkdirs();
			} catch (Exception ex) {
				throw new Exception("Erro ao criar o diretório de extração dos arquivos SIP.");
			}

		}

		String absolutePath = theDir.getAbsolutePath();

		return absolutePath;

	}

	/**
	 * Pega a Data/Hora da Carga do Arquivo iterando por cada um dos usuários existentes para a concessionária em questão. Se achar em qualquer um deles já retornar, não vai até o fim. Se não teve carga para nenhum dos usuários, estão retorna null.
	 * 
	 * @param driver
	 * @return Date
	 * @throws InterruptedException
	 * @throws ParseException
	 */
	private static Date getDataHoraCargaArquivo(WebDriver driver) throws InterruptedException, ParseException {

		// Pega o combo com os usuários
		WebElement comboUsuarios = driver.findElement(By.id("formEmp:usuario"));

		// Tenta pegar a data/hora da carga do arquivo em cada um
		List<WebElement> listU = comboUsuarios.findElements(By.tagName("option"));
		for (WebElement optU : listU) {

			String vU = "";
			while (StringUtils.isEmpty(vU)) {
				try {
					vU = optU.getAttribute("value");
				} catch (org.openqa.selenium.StaleElementReferenceException e) {
				}
			}

			// ignora a opção '0'
			if (!StringUtils.equalsIgnoreCase(vU, "0")) {

				// seleciona o usuário
				optU.click();
				Thread.sleep(3000);

				// Tenta achar a data e se achar já retorna
				Date dataHoraArquivo = tryGetDataHoraByUser(driver);
				if (dataHoraArquivo != null) {
					return dataHoraArquivo;
				}

			}

		}

		return null;
	}

	/**
	 * Pega a Data/Hora da Carga do Arquivo considerando o usupário atualmente selecionado. Se não teve carga para o usuário selecionado retorna null.
	 * 
	 * @param driver
	 * @param optU
	 * @return
	 * @throws InterruptedException
	 * @throws ParseException
	 */
	private static Date tryGetDataHoraByUser(WebDriver driver) throws InterruptedException, ParseException {

		// Acessa o Analítico e aguarda carregar
		driver.get("http://sipnissan.com.br/Sip/jsf_pages/automobilistico/autAnalitico/autAnalitico.jsf?apenasPesquisa=false");
		Thread.sleep(2000);

		// pega a div que contem o form planejamento
		WebElement divPlanejamento = driver.findElement(By.id("formE:planejamento_content"));

		// clica para abrir o form planejamento, caso contrário não consegue ler a data
		WebElement aPlanejamento = driver.findElement(By.id("formE:planejamento_toggler"));
		aPlanejamento.findElement(By.tagName("span")).click();

		// tenta ler a data na <td> que contem ela
		WebElement td = divPlanejamento.findElement(By.cssSelector("td[width='40%']"));

		String dtHrStr = td != null ? StringUtils.trim(td.getText()) : "";
		Date parseDate = null;
		if (StringUtils.isNotEmpty(dtHrStr)) {
			parseDate = DateUtils.parseDate(dtHrStr, "dd/MM/yyyy HH:mm");
		}

		return parseDate;
	}

	/**
	 * Abre caixa de diálogo para pedir o Driver ao Usuário. Se não selecionar o correto, pega do caminho padrão que está na rede.
	 * 
	 * @return
	 */
	private static String getDriver() {

		JFileChooser fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		fc.setDialogTitle("Selecione o Driver do Google Chrome >>> 'chromedriver.exe'");
		fc.setMultiSelectionEnabled(false);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileFilter(new FileNameExtensionFilter("EXE FILES", "exe", "exe"));

		int aux = -1;
		while (aux == -1) {
			aux = fc.showOpenDialog(null);
		}

		File f = fc.getSelectedFile();

		String driverPath = f != null ? f.getAbsolutePath() : "";

		// Pega o caminho padrão do driver na rede se não selecionou o correto >>>>
		// Z:\SISTEMAS\Troca de Arquivos\WebDriver
		if (driverPath == null || !driverPath.endsWith("chromedriver.exe")) {
			driverPath = "Z:\\SISTEMAS\\Troca de Arquivos\\WebDriver\\chromedriver.exe";
		}

		return driverPath;

	}

	/**
	 * Faz o Login no SIP
	 * 
	 * @param driver
	 * @throws InterruptedException
	 */
	private static void login(WebDriver driver) throws InterruptedException {

		String url = "http://sipnissan.com.br/Sip/login.jsf";
		String user = "srodrigues";
		String pass = "a1";

		// Acessa a tela de Login do SIP
		driver.get(url);
		// Thread.sleep(3000);

		WebElement userEl = driver.findElement(By.id("j_idt11:Login"));
		userEl.sendKeys(user);

		WebElement passEl = driver.findElement(By.id("j_idt11:Senha"));
		passEl.sendKeys(pass);

		WebElement btEl = driver.findElement(By.id("j_idt11:j_idt19"));
		btEl.click();

	}

}
