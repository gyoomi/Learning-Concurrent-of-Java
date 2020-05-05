package com.concurrent.excel;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.InputStream;

/**
 * 事件驱动：超大excel读取示例
 *
 * @author Leon
 * @version 2020/5/5 20:07
 */
public class EventReadExcelDemo
{
	public static void main(String[] args) throws Exception
	{
		new EventReadExcelDemo().processOneSheet("d:\\test.xlsx");
		System.out.println("ok");
	}

	public void processOneSheet(String filename) throws Exception {
		OPCPackage pkg = OPCPackage.open(filename);
		XSSFReader r = new XSSFReader(pkg);
		SharedStringsTable sst = r.getSharedStringsTable();

		XMLReader parser = fetchSheetParser(sst);

		// 获得第一个sheet
		InputStream sheet2 = r.getSheet("rId1");
		InputSource sheetSource = new InputSource(sheet2);
		parser.parse(sheetSource);
		sheet2.close();
	}

	public XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException {
		XMLReader parser =
			XMLReaderFactory.createXMLReader(
				"org.apache.xerces.parsers.SAXParser"
			);
		ContentHandler handler = new SheetHandler(sst);
		parser.setContentHandler(handler);
		return parser;
	}

	private static class SheetHandler extends DefaultHandler {
		private SharedStringsTable sst;
		private String lastContents;
		private boolean nextIsString;

		private SheetHandler(SharedStringsTable sst) {
			this.sst = sst;
		}

		//元素开始时的handler
		public void startElement(String uri, String localName, String name,
		                         Attributes attributes) throws SAXException {
			// c => 单元格
			if(name.equals("c")) {
				System.out.print(attributes.getValue("r") + " - ");
				// 获取单元格类型
				String cellType = attributes.getValue("t");
				if(cellType != null && cellType.equals("s")) {
					nextIsString = true;
				} else {
					nextIsString = false;
				}
			}
			lastContents = "";
		}

		//元素结束时的handler
		public void endElement(String uri, String localName, String name)
			throws SAXException {
			if(nextIsString) {
				int idx = Integer.parseInt(lastContents);
				lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
				nextIsString = false;
			}

			// v => 单元格内容
			if(name.equals("v")) {
				System.out.println(lastContents);
			}
		}

		//读取元素间内容时的handler
		public void characters(char[] ch, int start, int length)
			throws SAXException {
			lastContents += new String(ch, start, length);
		}
	}
}
