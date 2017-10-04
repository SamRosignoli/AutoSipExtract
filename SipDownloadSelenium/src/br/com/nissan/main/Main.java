package br.com.nissan.main;

import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
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

	private static JavascriptExecutor js = null;

	private static String downloadFilepath;

	public static void main(String[] args) {

		// String tituloMessage = "Selenium SIP Download";
		String codDealer = "";
		String descDealer = "";

		// Manipula os arquivos Excel com Apache POI
		Excel excel = new Excel();

		try {

			// Verifica se o diretório já existe, caso contrário cria um novo
			downloadFilepath = checkDir();

			// deleta todos os arquivos existentes na pasta sempre que executar uma nova rodada
			FileUtils.cleanDirectory(new File(downloadFilepath));

			String driverPath = getDriverPath();
			System.setProperty("webdriver.chrome.driver", driverPath);

			// abre o Chrome já com as opções configuradas (Ex.: maximizado)
			driver = new ChromeDriver(getChromeOptions());

			// possibilita a execução de javascript
			// faz todas as operações através de javascript por ser mais robusto que o método driver.click()
			// o método driver.click() só funciona se estiver com a janela do browser ativa e com o elemento visível
			js = (JavascriptExecutor) driver;

			// faz o login
			login();
			Thread.sleep(2000);

			// Iteração em todas as concessionárias existentes no Select da página para baixar o arquivo analítico
			// Já que no Selenium não é possível acessar um WebElement depois de um refresh na página em uma iteração, guarda o Set de concessionárias antes para conseguir iterar depois.
			List<Concessionaria> set = optionsToDealerList();
			int ct = 0;
			for (Concessionaria conc : set) {

				codDealer = conc.getCodigo();
				descDealer = conc.getDescricao();

				// ignora se for a opção '33 - Nissan' ou a opção '1 - SIP Nissan'
				if (!StringUtils.equalsIgnoreCase(codDealer, "33") && !StringUtils.equalsIgnoreCase(codDealer, "1")) {

					if (ct++ > 0) {
						// para trocar de concessionária tem de obrigatoriamente clicar na home do SIP antes
						// js.executeScript("document.getElementById('j_idt29:j_idt30').click();");
						driver.get("http://sipnissan.com.br/Sip/jsf_pages/home.jsf");
						Thread.sleep(2000);
					}

					// Seleciona a concessionária e aguarda carregar
					js.executeScript("document.getElementById('formEmp:empresa').value = '" + codDealer + "';");
					js.executeScript("document.getElementById('formEmp:empresa').onchange();");
					Thread.sleep(2000);

					// Seleciona o usuário e Pega a Data/Hora da Carga do Arquivo
					// vai tentando até o último usuário, se não tiver retorna nulo/vazio
					Date dtHrArquivo = getDataHoraCargaArquivo();

					// Se não teve carga de arquivo, ignora e parte para o próximo
					if (dtHrArquivo != null) {

						System.out.println("Extraindo o arquivo da concessionária " + descDealer);
						System.out.println("Data/Hora da Carga do Arquivo: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(dtHrArquivo));

						// clica em pesquisar
						js.executeScript("document.getElementById('formE:modelButton').getElementsByTagName('a')[3].click();");

						// verifica se terminou a busca
						waitPesquisar();

						File xls = null;
						int count = 0;
						// vai tentar até 10 vezes fazer o download, caso contrario sai do loop para não ficar eternamente
						while (xls == null && count < 10) {

							try {
								// clica para fazer o download
								js.executeScript("document.getElementById('formE:j_idt945').parentElement.click();");
							} catch (Exception e) {
								System.out.println("download ainda em andamento para a concessionária " + descDealer);
								// se teve erro, ignora e espera mais 5 segundos
								// pode ocorrer de o download ainda estar em andamento
								// neste caso vai gerar erro em uma nova tentativa e por isso captura aqui
							} finally {
								Thread.sleep(5000);
							}

							// tenta renomear o excel depois do download se achá-lo
							// retorna null se não encontrá-lo
							xls = renomeiaXls(descDealer);

							if (xls != null) {
								// Se achou é porque o download terminou com sucesso
								// Então inclui a coluna com a Data/Hora de extração do Arquivo
								excel.incluirColunaDataHora(dtHrArquivo, xls);
							} else {
								// se for null pode ser o erro '500' no SIP ao tentar fazer download do arquivo
								// Neste caso, faz o navegador voltar e tenta o download de novo.
								boolean erro500 = isErro500();
								if (erro500) {
									driver.navigate().back();
								}
								Thread.sleep(3000);
							}
							count++;
						}

						boolean ok = (xls != null);
						System.out.println("download " + descDealer + (ok ? " ok!" : " erro de time out!"));
						System.out.println("");

					}

					if (dtHrArquivo == null) {
						System.out.println("Download " + descDealer + " não ocorreu por falta de carga do arquivo");
						System.out.println("");
					}

				}

			}

			// Gera o arquivo único depois de extrair tudo
			// File arquivoFinal = excel.gerarArquivoUnico();

			// TODO - salvar o arquivo final no diretorio de onde o BI vai ler

			System.out.println("Arquivo final do SIP gerado com sucesso!");

			// JOptionPane.showMessageDialog(null, "Arquivo final do SIP gerado com sucesso!", tituloMessage, JOptionPane.INFORMATION_MESSAGE);

		} catch (TimeoutException e) {
			// JOptionPane.showMessageDialog(null, "Erro de tempo de espera excedido: " + e.getMessage(), tituloMessage, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

		} catch (NoSuchElementException e) {
			// JOptionPane.showMessageDialog(null, "Erro ao tentar encontrar um elemento na página do SIP", tituloMessage, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

		} catch (ParseException e) {
			// JOptionPane.showMessageDialog(null, "Erro ao tentar ler a Data/Hora de carga do arquivo no dealer " + codDealer + " - " + descDealer, tituloMessage, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

		} catch (Exception e) {
			// JOptionPane.showMessageDialog(null, "Erro Indeterminado: " + e.getMessage(), tituloMessage, JOptionPane.ERROR_MESSAGE);
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
	 * Depois que clica em pesquisar, verifica através deste método se terminou a busca olhando se a TD 'Data da Pesquisa' foi preenchida.
	 */
	private static void waitPesquisar() {
		Object jsReturn = null;
		String dtPesquisa = null;
		while (StringUtils.isEmpty(dtPesquisa)) {
			try {
				jsReturn = js.executeScript("return document.getElementById('formE:planejamento_content').getElementsByTagName('td')[3].innerText;");
				dtPesquisa = (jsReturn != null && jsReturn instanceof String) ? StringUtils.trim((String) jsReturn) : "";
				Thread.sleep(1000);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * Renomeia o arquivo, deleta o antigo e adiciona uma coluna com data e hora
	 * 
	 * @param descDealer
	 * @param dtHrArquivo
	 * @author xl02926
	 * @return
	 */
	private static File renomeiaXls(String descDealer) {
		File folder = new File(downloadFilepath);
		File[] listOfFiles = folder.listFiles();
		for (File f : listOfFiles) {
			if (f.isFile()) {
				String fName = f.getName();

				// garante que não vai pegar arquivos que porventura tenham sido salvos 2x
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
	 * Salva todas as opções do combo de Dealers em um List para possibilitar a iteração em cada option depois.
	 * 
	 * Não permite repetidos / Usa List para garantir a ordem dos itens na lista.
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
	 * <b>Define as opções para abertura do browser.</b><br>
	 * Ex.:<br>
	 * - abrir já maximizado<br>
	 * - diretório padrão para downloads
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
	 * Verifica se o diretório já existe, caso contrário cria um novo na pasta raiz do usuáriuo.
	 * 
	 * Ex.: D:\LocalData\x888541\Sip Extract
	 * 
	 * @return String com o path do diretório criado/já existente
	 * @throws Exception
	 *             lança uma Exception caso não consiga criar o diretório no SO.
	 */
	private static String checkDir() throws Exception {

		String userHome = System.getProperty("user.home");

		File theDir = new File(userHome + "\\Sip Extract");

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
	 * Pega a Data/Hora da Carga do Arquivo iterando por cada um dos usuários existentes para a concessionária em questão.<br>
	 * Se achar em qualquer um deles já retornar, não vai até o fim. Se não teve carga para nenhum dos usuários, estão retorna null.
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

			// ignora a opção '0'
			if (!StringUtils.equalsIgnoreCase(codigo, "0")) {

				if (ct > 0) {
					// para trocar de usuário tem de obrigatoriamente clicar na home do SIP antes
					// driver.findElement(By.id("j_idt29:j_idt30")).click();
					js.executeScript("document.getElementById('j_idt29:j_idt30').click();");
					Thread.sleep(2000);
				}

				// seleciona o usuário
				js.executeScript("document.getElementById('formEmp:usuario').value = '" + codigo + "';");
				js.executeScript("document.getElementById('formEmp:usuario').onchange();");
				Thread.sleep(2000);

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
	 * Pega o driver diretamente dos resources do projeto e extrai o mesmo na pasta raiz do usuário no SO em questão.<br>
	 * 
	 * Ex.: 'C:\Users\Sidney Rodrigues\ChromeDriver\chromedriver.exe'
	 * 
	 * @return
	 * @throws Exception
	 */
	private static String getDriverPath() throws Exception {

		String userHome = System.getProperty("user.home");

		String diretorio = userHome + File.separator + "ChromeDriver";

		// cria o diretorio se ainda não existir
		File f = new File(diretorio);
		if (!f.exists()) {
			try {
				f.mkdirs();
			} catch (Exception ex) {
				throw new Exception("Não foi possível criar o diretório ChromeDriver no user.home >>> " + ex.getMessage());
			}
		}

		// copia o driver se ainda não existir
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
	private static void login() throws InterruptedException {

		// FIXME - Hardcode!!! verificar a possibilidade de criar um arquivo de configuração externo a aplicação
		String url = "http://sipnissan.com.br/Sip/login.jsf";
		String user = "srodrigues";
		String pass = "a1";

		driver.get(url);
		Thread.sleep(2000);

		js.executeScript("document.getElementById('j_idt11:Login').value = '" + user + "';");
		js.executeScript("document.getElementById('j_idt11:Senha').value = '" + pass + "';");
		js.executeScript("document.getElementById('j_idt11:j_idt19').click();");

	}

}
