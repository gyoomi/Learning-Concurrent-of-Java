/**
 * Copyright © 2020, Glodon Digital Supplier BU.
 * <p>
 * All Rights Reserved.
 */

package com.concurrent.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

/**
 * 超大excel写出示例
 *
 * @author Leon
 * @date 2020-05-06 11:23
 */
public class SxssfWriteExcelDemo
{

	public static void main(String[] args) throws Exception
	{
		// jvm运行参数： -Xms100M -Xmx100M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=d://dump.hprof
		SXSSFWorkbook wb = new SXSSFWorkbook();

		try
		{
			Sheet sheet = wb.createSheet();

			// 模拟写出100万条数据
			for (int i = 0; i < 1000000; i++)
			{
				Row curRow = sheet.createRow(i);
				Cell cell = curRow.createCell(0);
				cell.setCellValue(LocalDate.now().toString() + UUID.randomUUID().toString() + new Random().nextInt());
			}

			FileOutputStream fos = new FileOutputStream("d:\\test111.xlsx");
			wb.write(fos);
			fos.close();
		}
		finally
		{
			// 删除临时文件
			if( wb != null)
			{
				wb.dispose();
			}
		}

		System.out.println("ok");
	}
}
