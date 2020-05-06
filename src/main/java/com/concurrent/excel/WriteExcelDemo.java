/**
 * Copyright © 2020, Glodon Digital Supplier BU.
 * <p>
 * All Rights Reserved.
 */

package com.concurrent.excel;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.time.LocalDate;

/**
 * 超大excel写出示例
 *
 * @author Leon
 * @date 2020-05-06 11:23
 */
public class WriteExcelDemo
{

	public static void main(String[] args) throws Exception
	{
		XSSFWorkbook wb = new XSSFWorkbook();
		XSSFSheet sheet = wb.createSheet("sheet-test");

		// 模拟写出50万条数据
		for (int i = 0; i < 500000; i++)
		{
			XSSFRow curRow = sheet.createRow(i);
			XSSFCell cell = curRow.createCell(0);
			cell.setCellValue(LocalDate.now().toString());
		}

		FileOutputStream fos = new FileOutputStream("d:\\test111.xlsx");
		wb.write(fos);
		fos.close();
	}
}
