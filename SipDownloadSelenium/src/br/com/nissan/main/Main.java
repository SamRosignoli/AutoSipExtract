package br.com.nissan.main;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
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

	private static WebDriver driver = null;

	private static String downloadFilepath;

	public static void main(String[] args) {

		String tituloMessage = "Selenium SIP Download";
		String codDealer = "";
		String descDealer = "";

		try {

			// Verifica se o diretório já existe, caso contrário cria um novo
			downloadFilepath = checkDir();

			// deleta todos os arquivos existentes na pasta sempre que executar uma nova rodada
			deleteActualfiles();

			String driverPath = getDriverPath();
			System.setProperty("webdriver.chrome.driver", driverPath);

			// abre o Chrome já com as opções configuradas (Ex.: maximizado)
			driver = new ChromeDriver(getChromeOptions());

			// faz o login
			login();
			Thread.sleep(5000);

			// Iteração em todas as concessionárias existentes no Select da página para baixar o arquivo analítico
			// Já que no Selenium não é possível acessar um WebElement depois de um refresh na página em uma iteração, guarda o Set de concessionárias antes para conseguir iterar depois.
			List<Concessionaria> set = optionsToDealerList();
			int ct = 0;
			for (Concessionaria conc : set) {

				codDealer = conc.getCodigo();
				descDealer = conc.getDescricao();
				int idxDealer = conc.getIndex();
				// debug do samuca -----------
				/*if (!"62".equalsIgnoreCase(codDealer)){
					continue;
				}*/

				// ignora se for a opção '33 - Nissan' ou a opção '1 - SIP Nissan'
				if (!StringUtils.equalsIgnoreCase(codDealer, "33") && !StringUtils.equalsIgnoreCase(codDealer, "1")) {
					
					if(ct>0) {
						// para trocar de concessionária tem de obrigatoriamente clicar na home do SIP antes
						driver.findElement(By.id("j_idt29:j_idt30")).click();
						Thread.sleep(3000);
					}
					
					ct++;

					WebElement comboDealers = driver.findElement(By.id("formEmp:empresa"));
					WebElement optC = comboDealers.findElements(By.tagName("option")).get(idxDealer);

					String codigo = StringUtils.trim(optC.getAttribute("value"));
					System.out.println("debug checking codigo >>> " + codigo.equalsIgnoreCase(codDealer));

					String descricao = StringUtils.trim(optC.getText());
					System.out.println("check checking descricao >>> " + descricao.equalsIgnoreCase(descDealer));

					// Seleciona a concessionária e aguarda carregar
					optC.click();
					Thread.sleep(3000);

					// Seleciona o usuário e Pega a Data/Hora da Carga do Arquivo
					// vai tentando até o último usuário, se não tiver retorna nulo/vazio
					Date dtHrArquivo = getDataHoraCargaArquivo();
					Thread.sleep(3000);

					// Se não teve carga de arquivo, ignora e parte para o próximo
					if (dtHrArquivo != null) {

						String fileStr = descDealer + ".xls";
						System.out.println(fileStr);

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

						renomeiaAndAddColXls(descDealer, dtHrArquivo);

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

	/**
	 * Renomeia o arquivo, deleta o antigo e adiciona uma coluna com data e hora
	 * @param descDealer
	 * @param dtHrArquivo
	 * @author xl02926
	 */
	private static void renomeiaAndAddColXls(String descDealer, Date dtHrArquivo) {
		File folder = new File(downloadFilepath);
		File[] listOfFiles = folder.listFiles();
		for (File f : listOfFiles) {
			if (f.isFile()) {

				String fName = f.getName();
				System.out.println("File " + fName);

				if ("DWA".equalsIgnoreCase(StringUtils.left(fName, 3))) {
					File oldFile = new File(downloadFilepath + "\\" + fName);
					File newFile = new File(downloadFilepath + "\\" + descDealer + ".xls");
					oldFile.renameTo(newFile);
					// usar newFile com poi
					oldFile.delete();
					// alterar o nome
					// incluir coluna na direita
					// salvar
					
					Excel e = new Excel();
					e.incluirColunaDataHora(dtHrArquivo, newFile);
					
				}

			}
		}
	}

	/**
	 * Salva todas as opções do combo de Dealers em um List para possibilitar a iteração em cada option depois.
	 * 
	 * Não permite repetidos / Usa List para garantir a ordem dos itens na lista.
	 * 
	 * @return HashSet com os objetos Concessionaria
	 */
	private static ArrayList<Concessionaria> optionsToDealerList() {

		ArrayList<Concessionaria> list = new ArrayList<Concessionaria>();

		WebElement comboDealers = driver.findElement(By.id("formEmp:empresa"));

		List<WebElement> listOptions = comboDealers.findElements(By.tagName("option"));

		int ct = 0;
		for (WebElement option : listOptions) {

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

	private static void deleteActualfiles() {
		// TODO - deletar todos os arquivos existentes na pasta para não dar pau na lógica em produção
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
	 * Verifica se o diretório já existe, caso contrário cria um novo na pasta raiz do usuáriuo.
	 * 
	 * Ex.: D:\LocalData\x888541\Sip Extract
	 * 
	 * @return String com o path do diretório criado/já existente
	 * @throws Exception
	 *             lança uma Exception caso não consiga criar o diretório no SO.
	 */
	private static String checkDir() throws Exception {

		String downloadFilepath = System.getProperty("user.home");

		File theDir = new File(downloadFilepath + "\\Sip Extract");

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			try {
				theDir.mkdirs();
			} catch (Exception ex) {
				throw new Exception("Não foi possível criar o diretório para extração dos arquivos SIP >>> " + ex.getMessage());
			}

		}

		String absolutePath = theDir.getAbsolutePath();

		return absolutePath;

	}

	/**
	 * Pega a Data/Hora da Carga do Arquivo iterando por cada um dos usuários existentes para a concessionária em questão. Se achar em qualquer um deles já retornar, não vai até o fim. Se não teve carga
	 * para nenhum dos usuários, estão retorna null.
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
			String nome = u.getNome();
			int index = u.getIndex();

			// ignora a opção '0'
			if (!StringUtils.equalsIgnoreCase(codigo, "0")) {
				
				if(ct>0) {
					// para trocar de usuário tem de obrigatoriamente clicar na home do SIP antes
					driver.findElement(By.id("j_idt29:j_idt30")).click();
					Thread.sleep(3000);
				}

				System.out.println("Tentativa de pegar e Data/Hora da Carga com o seguinte usuário: " + codigo + " - " + nome);

				WebElement comboUsuarios = driver.findElement(By.id("formEmp:usuario"));
				WebElement optU = comboUsuarios.findElements(By.tagName("option")).get(index);

				String vU = optU.getAttribute("value");
				System.out.println("debug checking codigo user >>> " + vU.equalsIgnoreCase(codigo));

				// seleciona o usuário
				optU.click();
				Thread.sleep(3000);

				// Tenta achar a data e se achar já retorna, não vai para o próximo
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
	 * Salva todas as opções do combo de Users em um List para possibilitar a iteração em cada option depois.
	 * 
	 * Não permite repetidos / Usa List para garantir a ordem dos itens na lista.
	 * 
	 * @return
	 */
	private static List<User> optionsToUserList() {

		ArrayList<User> list = new ArrayList<User>();

		WebElement comboUsuarios = driver.findElement(By.id("formEmp:usuario"));

		List<WebElement> userOptions = comboUsuarios.findElements(By.tagName("option"));
		
		int ct = 0;
		for (WebElement opt : userOptions) {

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
	 * Pega a Data/Hora da Carga do Arquivo considerando o usupário atualmente selecionado. Se não teve carga para o usuário selecionado retorna null.
	 * 
	 * @param driver
	 * @param optU
	 * @return
	 * @throws InterruptedException
	 * @throws ParseException
	 */
	private static Date tryToGetDataHoraByUser() throws InterruptedException, ParseException {

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
	private static String getDriverPath() {

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
		// FIXME - Hard Code do mal!!!!
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
	private static void login() throws InterruptedException {

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
