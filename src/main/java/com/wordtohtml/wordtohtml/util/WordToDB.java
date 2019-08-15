package com.wordtohtml.wordtohtml.util;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.StyleDescription;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import java.io.*;

public class WordToDB {

    public static void main(String[] args) throws Exception {
        String filePath = "F:\\file\\FCW-05-01-02-02.doc";
        printWord(filePath);       
    }

    public static void printWord(String filePath) throws IOException {
        InputStream is = new FileInputStream(filePath);
        HWPFDocument doc = new HWPFDocument(is);
        Range r = doc.getRange();// 文档范围

        // System.out.println("段落数："+r.numParagraphs());

        for (int i = 0; i < r.numParagraphs(); i++) {

            
            Paragraph p = r.getParagraph(i);// 获取段落

            int numStyles = doc.getStyleSheet().numStyles();

            int styleIndex = p.getStyleIndex();

            if (numStyles > styleIndex) {
                StyleSheet style_sheet = doc.getStyleSheet();
                StyleDescription style = style_sheet.getStyleDescription(styleIndex);

                String styleName = style.getName();// 获取每个段落样式名称

                // 获取自己理想样式的段落文本信息
                String styleLoving = "级别2：四号黑体 20磅 前18 后12 左对齐";
                if (styleName != null && styleName.contains(styleLoving)) {
                    String text = p.text();// 段落文本
                    System.out.println(text);
                }
            }
        }

    }
}