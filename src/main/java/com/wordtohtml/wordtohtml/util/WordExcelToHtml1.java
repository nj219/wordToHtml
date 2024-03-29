package com.wordtohtml.wordtohtml.util;

import java.io.*;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.sun.xml.internal.bind.v2.TODO;
import jdk.internal.org.objectweb.asm.commons.RemappingAnnotationAdapter;
import org.apache.http.entity.ContentType;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class WordExcelToHtml1 {
    /**
     * 回车符ASCII码
     */
    private static final short ENTER_ASCII = 13;
    /**
     * 空格符ASCII码
     */
    private static final short SPACE_ASCII = 32;

    /**
     * 水平制表符ASCII码
     */
    private static final short TABULATION_ASCII = 9;

    public static String htmlText = "";
    public static String htmlTextTbl = "";
    public static int counter = 0;
    public static int beginPosi = 0;
    public static int endPosi = 0;
    public static int beginArray[];
    public static int endArray[];
    public static String htmlTextArray[];
    public static boolean tblExist = false;

    public static final String inputFile = "F:\\file\\FCW-05-01-02-02.doc";


    public static void main() {
        try {
            getWordAndStyle(inputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取每个文字样式
     *
     * @param fileName
     * @throws Exception
     */

    public static void getWordAndStyle(String fileName) throws Exception {
        FileInputStream in = new FileInputStream(new File(fileName));
        HWPFDocument doc = new HWPFDocument(in);
        Range rangetbl = doc.getOverallRange();// 得到文档的读取范围
        //TableIterator it = new TableIterator(rangetbl);
        //boolean b = it.hasNext();
        int num = 1024;
        beginArray = new int[num];
        endArray = new int[num];
        htmlTextArray = new String[num];
        // 取得文档中字符的总数
        int length = doc.characterLength();
        // 创建图片容器
        PicturesTable pTable = doc.getPicturesTable();
        htmlText = "<html><head><title>"
                + doc.getSummaryInformation().getTitle()
                + "</title><meta charset=\"UTF-8\"></head><body>";
        // 创建临时字符串,好加以判断一串字符是否存在相同格式

        int cur = 0;

        String tempString = "";
        for (int i = 0; i < length - 1; i++) {
            //表格
            TableIterator it = new TableIterator(rangetbl);
            if (it.hasNext()) {
                readTable(it, rangetbl);
            }

            // 整篇文章的字符通过一个个字符的来判断,range为得到文档的范围
            Range range = new Range(i, i + 1, doc);
            CharacterRun cr = range.getCharacterRun(0);

            if (tblExist) {
                if (i == beginArray[cur]) {
                    htmlText += tempString + htmlTextArray[cur];
                    tempString = "";
                    i = endArray[cur] - 1;
                    cur++;
                    continue;
                }
            }
            if (pTable.hasPicture(cr)) {
                htmlText += tempString;
                // 读写图片
                readPicture(pTable, cr);
                tempString = "";
            } else {

                Range range2 = new Range(i + 1, i + 2, doc);
                // 第二个字符
                CharacterRun cr2 = range2.getCharacterRun(0);
                String text = cr.text();
                char c = cr.text().charAt(0);

                System.out.println(i + "::" + range.getEndOffset() + "::"
                        + range.getStartOffset() + "::" + c);

                // 判断是否为回车符
                if (c == ENTER_ASCII) {
                    tempString += "<br/>";
                }
                // 判断是否为空格符
                else if (c == SPACE_ASCII)
                    tempString += " ";
                    // 判断是否为水平制表符
                else if (c == TABULATION_ASCII)
                    tempString += "    ";


                // 比较前后2个字符是否具有相同的格式
                boolean flag = compareCharStyle(cr, cr2);
                if (flag)
                    tempString += cr.text();
                else {
                    //font-size: " + cr.getFontSize() / 2 + ";
                    String fontStyle = "<span style='font-family: " + cr.getFontName() + ";";

                    if (cr.isBold()) {
                        fontStyle += "font-weight:bold;";
                    }

                    if (cr.isItalic()) {
                        fontStyle += "font-style:italic;";
                    }

                    if (cr.getColor() == 6) {
                        fontStyle += "color: red;";
                    }

                    //color = cr.getColor();

                    htmlText += fontStyle;

                    /*if (cr.isBold())
                        fontStyle += "font-weight:bold;";
                    if (cr.isItalic())
                        fontStyle += "font-style:italic;";

                    htmlText += fontStyle;*/
                    htmlText += " '>";
                    tempString += cr.text();
                    htmlText += tempString + "</span>";
                    tempString = "";
                }
            }
        }
        htmlText += "</body></html>";
        writeFile(htmlText);
    }

    /**
     * 读写文档中的表格
     *
     * @param
     * @throws Exception
     */
    public static void readTable(TableIterator it, Range rangetbl)
            throws Exception {
        htmlTextTbl = "";
        // 迭代文档中的表格
        counter = -1;
        while (it.hasNext()) {
            tblExist = true;
            htmlTextTbl = "";
            Table tb = (Table) it.next();
            beginPosi = tb.getStartOffset();
            endPosi = tb.getEndOffset();
            counter = counter + 1;
            // 迭代行，默认从0开始
            beginArray[counter] = beginPosi;
            endArray[counter] = endPosi;
            htmlTextTbl += "<table border cellspacing=\"0\" cellpadding=\"0\">";
            for (int i = 0; i < tb.numRows(); i++) {
                TableRow tr = tb.getRow(i);

                htmlTextTbl += "<tr>";
                // 迭代列，默认从0开始
                for (int j = 0; j < tr.numCells(); j++) {
                    TableCell td = tr.getCell(j);// 取得单元格
                    int cellWidth = td.getWidth();

                    // 取得单元格的内容
                    for (int k = 0; k < td.numParagraphs(); k++) {
                        Paragraph para = td.getParagraph(k);
                        String s = para.text().toString().trim();
                        if (s == "") {
                            s = " ";
                        }
                        htmlTextTbl += "<td width=" + cellWidth + ">" + s
                                + "</td>";
                    }
                }
            }
            htmlTextTbl += "</table>";
            htmlTextArray[counter] = htmlTextTbl;
        }
    }

    private static String url;

    @Value("${upload.interface}")
    public void setUrl(String url) {
        WordExcelToHtml1.url = url;
    }

    private static String uploadUrl;

    @Value("${uploadUrl}")
    public void setUploadUrl(String uploadUrl) {
        WordExcelToHtml1.uploadUrl = uploadUrl;
    }

    /**
     * 读写文档中的图片
     *
     * @param pTable
     * @param cr
     * @throws Exception
     */
    public static void readPicture(PicturesTable pTable, CharacterRun cr)
            throws Exception {
        // 提取图片
        Picture pic = pTable.extractPicture(cr, false);
        // 返回POI建议的图片文件名
        String afileName = pic.suggestFullFileName();

        String upUrl = null;
        try {
            String type = afileName.split("[.]")[1];

            byte[] data = pic.getContent();
            upUrl = "";

            InputStream inputStream = new ByteArrayInputStream(data);
            MultipartFile file = new MockMultipartFile("new." + type, "old." + type, ContentType.APPLICATION_OCTET_STREAM.toString(), inputStream);

            upUrl = HttpClientUtils.httpClientUploadFile(url, file);

            Map<String, String> parse = (Map<String, String>) JSONObject.parse(upUrl);
            for (String value : parse.values()) {
                upUrl = value;
                break;
            }
        } catch (Exception e) {
            htmlText += "<img src='" + uploadUrl + upUrl + "'/>";
        }
        htmlText += "<img src='" + uploadUrl + upUrl + "'/>";

    }

    public static boolean compareCharStyle(CharacterRun cr1, CharacterRun cr2) {
        boolean flag = false;
        if (cr1.isBold() == cr2.isBold() && cr1.isItalic() == cr2.isItalic()
                && cr1.getFontName().equals(cr2.getFontName())
                && cr1.getFontSize() == cr2.getFontSize()
                && cr1.getColor() == cr2.getColor()) {
            flag = true;
        }
        return flag;
    }

    /**
     * 写文件
     *
     * @param s
     */

    public static void writeFile(String s) {
        FileOutputStream fos = null;
        BufferedWriter bw = null;
        try {
            File file = new File("f://abc.html");
            fos = new FileOutputStream(file);
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(s);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fos != null)
                    fos.close();
            } catch (IOException ie) {
            }
        }
    }

}