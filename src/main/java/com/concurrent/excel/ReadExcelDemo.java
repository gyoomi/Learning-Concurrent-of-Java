package com.concurrent.excel;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;

/**
 * 超大excel读取示例
 *
 * @author Leon
 * @version 2020/5/5 20:07
 */
public class ReadExcelDemo
{
	public static void main(String[] args) throws Exception
	{
		FileInputStream is = new FileInputStream("d:\\test.xlsx");
		XSSFWorkbook wb = new XSSFWorkbook(is);
		// TODO with wb
		System.out.println("ok");
	}
}
