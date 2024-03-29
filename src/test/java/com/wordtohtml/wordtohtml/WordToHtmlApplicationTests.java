package com.wordtohtml.wordtohtml;

import com.wordtohtml.wordtohtml.util.DocToHtml;
import com.wordtohtml.wordtohtml.util.DocxToHtml;
import com.wordtohtml.wordtohtml.util.WordExcelToHtml1;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WordToHtmlApplicationTests {

    @Test
    public void contextLoads() {
        /*try {
            WordExcelToHtml1.getWordAndStyle("F:\\file\\FCW-14-01-18-01 .doc");
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        /*try {
            DocToHtml.convert2Html("F:\\file\\FCW-04-01-02-09.doc", "F:\\file\\doc.html");
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }*/

        try {
            DocxToHtml.docx2Html("F:/file/FCW-14-02-01-04.docx", "F:/file/docx.html");
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*try {
            //XWPFUtils.getImgUrl("F:/file/测试.docx");
            DocxToHtml.docx2Html("F:/file/测试.docx", "F:/file/docx.html");
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

}
