package br.com.nissan.main;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
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

	public static void main(String[] args) {

		cargas = new HashMap<>();
		cargas.clear();

		WebDriver driver = null;

		String tituloMessage = "Selenium SIP Download";
		String codDealer = "";
		String descDealer = "";

		try {

			String driverPath = getDriver();
			System.setProperty("webdriver.chrome.driver", driverPath);

			// abre o Chrome j� com as op��es configuradas (Ex.: maximizado)
			driver = new ChromeDriver(getChromeOptions());

			// faz o login
			login(driver);
			Thread.sleep(5000);

			// Itera��o em todas as concesion�rias existentes no Select da p�gina para baixar o arquivo anal�tico
			// Ignora a op��o 33 - Nissan
			// Somente exporta os dados das concession�rias
			WebElement comboDealers = driver.findElement(By.id("formEmp:empresa"));
			List<WebElement> list = comboDealers.findElements(By.tagName("option"));
			for (WebElement optC : list) {

				codDealer = optC.getAttribute("value");
				descDealer = optC.getText();

				// ignora se for Nissan
				if (!StringUtils.equalsIgnoreCase(codDealer, "33") && !StringUtils.equalsIgnoreCase(codDealer, "1")) {

					// Seleciona a concession�ria e aguarda carregar
					optC.click();
					Thread.sleep(3000);

					// Seleciona o usu�rio e Pega a Data/Hora da Carga do Arquivo
					// vai tentando at� o �ltimo usu�rio, se n�o tiver retorna nulo/vazio
					Date dtHrArquivo = getDataHoraCargaArquivo(driver);
					Thread.sleep(3000);

					// Se n�o teve carga de arquivo, ignora e parte para o pr�ximo
					if (dtHrArquivo != null) {

						String fileStr = descDealer + ".xls";
						System.out.println(fileStr);

						cargas.put(codDealer, dtHrArquivo);

						// clica em pesquisar
						WebElement pesquisar = driver.findElement(By.id("formE:modelButton")).findElements(By.tagName("a")).get(3);
						pesquisar.click();
						Thread.sleep(5000);

					}

				}

			}

			JOptionPane.showMessageDialog(null, "Arquivo final do SIP gerado com sucesso!", tituloMessage, JOptionPane.INFORMATION_MESSAGE);

		} catch (TimeoutException e) {
			JOptionPane.showMessageDialog(null, "Erro de tempo de espera excedido: " + e.getMessage(), tituloMessage, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

		} catch (NoSuchElementException e) {
			JOptionPane.showMessageDialog(null, "Erro ao tentar encontrar um elemento na p�gina do SIP", tituloMessage, JOptionPane.ERROR_MESSAGE);
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

	/**
	 * Op��es para abertura do browser.
	 * Ex.: abrir j� maximizado
	 * @return
	 */
	private static ChromeOptions getChromeOptions() {
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("--start-maximized");
		return chromeOptions;
	}

	/**
	 * Pega a Data/Hora da Carga do Arquivo iterando por cada um dos usu�rios existentes para a concession�ria em quest�o. Se achar em qualquer um deles j� retornar, n�o vai at� o fim. Se n�o teve carga
	 * para nenhum dos usu�rios, est�o retorna null.
	 * 
	 * @param driver
	 * @return Date
	 * @throws InterruptedException
	 * @throws ParseException
	 */
	private static Date getDataHoraCargaArquivo(WebDriver driver) throws InterruptedException, ParseException {

		// Pega o combo com os usu�rios
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

			// ignora a op��o '0'
			if (!StringUtils.equalsIgnoreCase(vU, "0")) {

				// seleciona o usu�rio
				optU.click();
				Thread.sleep(3000);

				// Tenta achar a data e se achar j� retorna
				Date dataHoraArquivo = tryGetDataHoraByUser(driver);
				if (dataHoraArquivo != null) {
					return dataHoraArquivo;
				}

			}

		}

		return null;
	}

	/**
	 * Pega a Data/Hora da Carga do Arquivo considerando o usup�rio atualmente selecionado. Se n�o teve carga para o usu�rio selecionado retorna null.
	 * 
	 * @param driver
	 * @param optU
	 * @return
	 * @throws InterruptedException
	 * @throws ParseException
	 */
	private static Date tryGetDataHoraByUser(WebDriver driver) throws InterruptedException, ParseException {

		// Acessa o Anal�tico e aguarda carregar
		driver.get("http://sipnissan.com.br/Sip/jsf_pages/automobilistico/autAnalitico/autAnalitico.jsf?apenasPesquisa=false");
		Thread.sleep(2000);

		// pega a div que contem o form planejamento
		WebElement divPlanejamento = driver.findElement(By.id("formE:planejamento_content"));

		// clica para abrir o form planejamento, caso contr�rio n�o consegue ler a data
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
	 * Abre caixa de di�logo para pedir o Driver ao Usu�rio. Se n�o selecionar o correto, pega do caminho padr�o que est� na rede.
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

		// Pega o caminho padr�o do driver na rede se n�o selecionou o correto >>>> Z:\SISTEMAS\Troca de Arquivos\WebDriver
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
