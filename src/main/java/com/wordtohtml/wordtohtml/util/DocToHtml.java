package com.wordtohtml.wordtohtml.util;

import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.PicturesManager;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.model.*;
import org.apache.poi.hwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URLDecoder;
import java.util.List;

/**
 * @ClassName docToHtml
 * @Author shangpinyuan06
 * @Date 2019/8/5 15:43
 **/
@Component
public class DocToHtml {
    /**
     * 解析doc
     * @param fileName  文件地址
     * @param outPutFile  输出地址
     * @throws TransformerException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public static void convert2Html(String fileName, String outPutFile)
            throws TransformerException, IOException,
            ParserConfigurationException {
        HWPFDocument wordDocument = new HWPFDocument(new FileInputStream(fileName));

        /*StringBuilder text = wordDocument.getText();
        String s = text.toString();
        String[] split = s.split("[《]");
        for (int i = 0; i < split.length; i++) {
            String[] split1 = split[i].split("[》]");

            for (int j = 0; j < split1.length; j++) {

                System.out.println(split1[j]);

            }
        }*/


        DocumentSummaryInformation documentSummaryInformation = wordDocument.getDocumentSummaryInformation();
        String documentText = wordDocument.getDocumentText();

        HeaderStories headerStories = new HeaderStories(wordDocument);
        String oddHeader = headerStories.getOddHeader();

        Range overallRange = wordDocument.getOverallRange(); //页眉页脚

        WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .newDocument());

        wordToHtmlConverter.setPicturesManager(new PicturesManager() {
            public String savePicture(byte[] content,
                                      PictureType pictureType, String suggestedName,
                                      float widthInches, float heightInches) {
                return "" + suggestedName;
            }
        });


        wordToHtmlConverter.processDocumentPart(wordDocument, overallRange);
        //wordToHtmlConverter.processDocument(wordDocument);
        //save pictures
        List pics = wordDocument.getPicturesTable().getAllPictures();
        if (pics != null) {
            for (int i = 0; i < pics.size(); i++) {
                Picture pic = (Picture) pics.get(i);

                try {
                    pic.writeImageContent(new FileOutputStream("F:/file/"
                            + pic.suggestFullFileName()));

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        Document htmlDocument = wordToHtmlConverter.getDocument();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DOMSource domSource = new DOMSource(htmlDocument);
        StreamResult streamResult = new StreamResult(out);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.METHOD, "html");
        serializer.transform(domSource, streamResult);
        out.close();
        writeFile(new String(out.toByteArray()), outPutFile);
    }

    /**
     * 修改页眉页脚位置
     * @param content
     * @param path
     * @param
     */
    private static void writeFile(String content, String path) {
        //利用jsoup解析HTML
        org.jsoup.nodes.Document doc = Jsoup.parse(content);



        Elements table = doc.getElementsByTag("table");

        Elements div = doc.getElementsByTag("div");

        Element node = table.get(table.size() - 1);

        //获取解析后的页眉，与之前做对比，如果相同，则提取
        String text = node.text().replaceAll("\\s", "");

        //String s1 = header.replaceAll("\\s", "").replaceAll("\r", "");

        //把页眉替换到头部
        /*Element child = div.first().child(0);
        child.before(node.toString());
        //删除多余页眉
        node.remove();*/

        //遍历取出图片
        Elements img = doc.select("img");
        for (Element element : img) {
            String src = element.attr("src");

            if (src.endsWith("emf")) {

            }
        }


        //遍历取出所有a标签，解析href，替换为自己的接口,访问接口去数据库对比是否存在文件
        Elements e = doc.select("a");
        for (int i = 0; i < e.size(); i++) {
            Element anode = e.get(i);
            String href = anode.attr("href");

            if (href.startsWith("http") || href.startsWith("https")) {

            } else {
                try {
                    href = URLDecoder.decode(href, "UTF-8");
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
                String[] split = href.split("/");
                String s = split[split.length - 1];

                href = "http://localhost/test/wordDoc/?word=" + s; //修改style中的url值
            }


            anode.attr("href", href);
        }

        FileOutputStream fos = null;
        BufferedWriter bw = null;
        try {
            File file = new File(path);
            fos = new FileOutputStream(file);
            bw = new BufferedWriter(new OutputStreamWriter(fos, "utf-8"));

            bw.write(doc.toString());
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
