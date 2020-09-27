package com.company;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.*;

public class Main {

	// Парсинг из html документа с пакета , а именно поиск whl#
	private static class XMLHandler extends DefaultHandler {
		String url_package;

		// Описываю логику реакции на начало элемента "<a>"

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

			if (qName.equals("a")) // находим тэг a
				if (attributes.getValue("href").contains("whl#")) //ищем ссылку с whl#
					url_package = attributes.getValue("href"); //забираем ссылку

		}
	}

	public static void downloadFileFromSimple(String urlFile,String nameFile) throws IOException // Сама подгрузка
	{
		URL url =new URL(urlFile); // указываем URL

		FileOutputStream fos= new FileOutputStream(nameFile); // поток файла

		ReadableByteChannel rbc = Channels.newChannel(url.openStream()); // создаём поток для чтения байтового канала

		fos.getChannel().transferFrom(rbc,0,Long.MAX_VALUE); // загрузка html файла

		fos.close();
		rbc.close();
	}

	// Строим Граф

	public static void getCodeForGraphViz (File newLib,String nameRes,Scanner mtScan) throws ParserConfigurationException, SAXException, IOException
	{
		String line;
		while(mtScan.hasNextLine()) // Вывод зависимостей и их вывод
		{
			line =mtScan.nextLine();
			if(line.contains("Requires-Dist:") && !line.contains("extra")) {
				line = line.substring(line.indexOf(' ') + 1, line.length());
					if(line.indexOf(' ') != -1)
						line = line.substring(0, line.indexOf(' '));
					System.out.println("\"" + nameRes + "\"" + " -> " + "\"" + line + "\""); // выводим зависимость
					getPackageRequirementsApplication(newLib,nameRes);
			}


		}

	}

	// Разархивация

	public static File getSourcesFromZip(File newLib,String nameRes) throws ParserConfigurationException, SAXException, IOException
	{

		// Путь к METADATA

		String metaDataWay;
		metaDataWay = null;

		// Создаём файлы для whl и rar

		File whlWay= new File(newLib.getPath() + "/" + nameRes + ".whl"); //Файл whl
		File rarWay= new File(newLib.getPath() + "/" + nameRes); //Файл раровский
		rarWay.mkdir();
		extractFolder(whlWay.getPath(),rarWay.getPath());
		return rarWay;
	}

	// Подключаю основное расширение для поиска зависимостей и в сути самую главную часть

	public static void getPackageRequirementsApplication(File newLib,String nameRes) throws ParserConfigurationException, SAXException, IOException {

		SAXParserFactory factory = SAXParserFactory.newInstance(); // создал фабрику для парсера , т.к. того требует сам парсер
		SAXParser parser =factory.newSAXParser(); // а вот непосредственно и сам парсер

		String urlDirectionOfPackage; // создаю путь до библиотеки на "https://pypi.org/simple/"
		urlDirectionOfPackage = "https://pypi.org/simple/" + nameRes + "/" ; // собственно заполняю сам путь с помощтю переданных данных

		// надо подргузить файл (на черновике я пробовал немного иначе , но в итоге загуглил в интернете как подгружать файл, так как за имением директория ещё ничего не сделано

		XMLHandler handler =new XMLHandler(); // создаём Хендлер

		downloadFileFromSimple(urlDirectionOfPackage,newLib.getPath() + "/" + nameRes + ".html"); // подгрузка htm документа

		parser.parse(new File(newLib.getPath() + "/" + nameRes + ".html"),handler); // парсим по файлу ссылку

		downloadFileFromSimple(handler.url_package,newLib.getPath() + "/" + nameRes + ".whl"); // скачиваем и получаем архив

		// Как разархивировать ??

		File rarWay = getSourcesFromZip(newLib,nameRes); //разархивировали и присвоили файл архива

		// Путь к METADATA



		String line;
		String metaDir = null; // Путь для меты

		for(String path :rarWay.list()) // Покуда не закончатся строчки
		{

			if (path.contains("dist-info")) // в папке точно dist-info имеем Metadata с Зависимостями
				metaDir = newLib.getPath() + "/" + nameRes + "/" + path + "/METADATA"; // создаём путь к METADATA
		}

		File mtData =new File(metaDir);
		Scanner scan = new Scanner(mtData);

		//getCodeForGraphViz(newLib,nameRes,mtScan);

		while(scan.hasNextLine()){
			line = scan.nextLine();
			if (line.contains("Requires-Dist:") && !line.contains("extra")){
				line = line.substring(line.indexOf(' ') + 1, line.length());
				if(line.indexOf(' ') != -1)
					line = line.substring(0, line.indexOf(' '));
				System.out.println("\"" + nameRes + "\"" + " -> " + "\"" + line + "\""); // выводим зависимость
				getPackageRequirementsApplication(newLib,line);
			}


		}
	}

	public static void deletePackageRequirementsApplication(String daWay) // Удаление папки
	{
		File file2Delete = new File(daWay);
		if (file2Delete.exists())
		{
			if (file2Delete.isDirectory())
			{
				if (file2Delete.list().length>0)
				{
					for(String s : file2Delete.list())
					{
						deletePackageRequirementsApplication(daWay + "/" + s);
					}
				}
				file2Delete.delete();
			}
		}


	}



    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
	Scanner scan1 = new Scanner(System.in);

	String nameRes;
	nameRes = scan1.next(); // наша библиотека нужная

	String downIn ="C:/Users/Asus/Desktop/cfg_hw_xml/"; // куда я её скачиваю

		File newLib = new File(downIn + nameRes); // создаём пространство для библиотеки

		newLib.mkdir(); // mkdir создёт папку через адресс

		String topAnswer =( "digraph G {"); // создаём шапку нашего ответа

		System.out.println(topAnswer); // выводим шапку в ответ

		getPackageRequirementsApplication(newLib,nameRes);
		System.out.println("}");
		deletePackageRequirementsApplication(newLib.getPath());
    }

	public static void extractFolder(String zipFile,String extractFolder)
	{
		try
		{
			int BUFFER = 2048;
			File file = new File(zipFile);

			ZipFile zip = new ZipFile(file);
			String newPath = extractFolder;

			new File(newPath).mkdir();
			Enumeration zipFileEntries = zip.entries();

			// Process each entry
			while (zipFileEntries.hasMoreElements())
			{
				// grab a zip file entry
				ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
				String currentEntry = entry.getName();

				File destFile = new File(newPath, currentEntry);
				//destFile = new File(newPath, destFile.getName());
				File destinationParent = destFile.getParentFile();

				// create the parent directory structure if needed
				destinationParent.mkdirs();

				if (!entry.isDirectory())
				{
					BufferedInputStream is = new BufferedInputStream(zip
							.getInputStream(entry));
					int currentByte;
					// establish buffer for writing file
					byte data[] = new byte[BUFFER];

					// write the current file to disk
					FileOutputStream fos = new FileOutputStream(destFile);
					BufferedOutputStream dest = new BufferedOutputStream(fos,
							BUFFER);

					// read and write until last byte is encountered
					while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
						dest.write(data, 0, currentByte);
					}
					dest.flush();
					dest.close();
					is.close();
				}


			}
		}
		catch (Exception e)
		{
			System.out.println("ERROR: "+e.getMessage());
		}

	}

}
