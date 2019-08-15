package com.wordtohtml.wordtohtml.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.PicturesManager;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.converter.WordToHtmlUtils;
import org.apache.poi.hwpf.model.DocumentProperties;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.model.io.HWPFOutputStream;
import org.apache.poi.hwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName docToHtml
 * @Author shangpinyuan06
 * @Date 2019/8/5 15:43
 **/
@Component
@Slf4j
public class DocToHtml {
    private static String url;

    @Value("${upload.interface}")
    public void setUrl(String url) {
        DocToHtml.url = url;
    }

    private static String uploadUrl;

    @Value("${uploadUrl}")
    public void setUploadUrl(String uploadUrl) {
        DocToHtml.uploadUrl = uploadUrl;
    }

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

        Range overallRange = wordDocument.getOverallRange(); //页眉页脚

        HeaderStories headerStories = new HeaderStories(wordDocument);
        String header = headerStories.getHeader(0);

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
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        if (pics != null) {
            for (int i = 0; i < pics.size(); i++) {
                Picture pic = (Picture) pics.get(i);
                String picName = pic.suggestFullFileName();
                //图片处理
                String upUrl = "";
                try {
                    String type = picName.split("[.]")[1];
                    byte[] data = pic.getContent();
                    InputStream inputStream = new ByteArrayInputStream(data);
                    MultipartFile file = new MockMultipartFile("new." + type, "old." + type, ContentType.APPLICATION_OCTET_STREAM.toString(), inputStream);

                    upUrl = HttpClientUtils.httpClientUploadFile(url, file);

                    Map<String, String> parse = (Map<String, String>) JSONObject.parse(upUrl);
                    for (String value : parse.values()) {
                        upUrl = value;
                        break;
                    }

                    linkedHashMap.put(picName, uploadUrl + upUrl);
                } catch (Exception e) {
                    log.info("------------------------------页眉图片解析异常-------------------------------------------");
                    throw new RuntimeException("页眉图片解析异常");
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
        writeFile(new String(out.toByteArray()), linkedHashMap, header);
    }

    /**
     * 修改页眉页脚位置
     * @param content
     * @param linkedHashMap
     * @param
     */
    private static void writeFile(String content, LinkedHashMap<String, String > linkedHashMap, String header) {
        //需要新增《》链接解析，增加<a>标签,说明：
        //只作为上传旧文件解析时使用，后期上传文件走正常流程!!!!
        //如果使用需要改进对资源的浪费
        //需要对a标签进行解析
        String oldHtml = content;
        String endHtml = "";
        for (int i = 0; i < oldHtml.length(); i++) {
            String substring = oldHtml.substring(i, i + 1);
            endHtml += substring;
            if (substring.equals("《")) {
                endHtml += "<a>";

            }
            if (substring.equals("》")) {
                endHtml += "</a>";
            }
        }

        //利用jsoup解析HTML
        org.jsoup.nodes.Document doc = Jsoup.parse(endHtml);

        //取出图片替换链接
        Elements img1 = doc.getElementsByTag("img");
        for (Element img : img1) {
            String src = img.attr("src");

            for (String key : linkedHashMap.keySet()) {
                if (src.equals(key)) {
                    img.attr("src", linkedHashMap.get(key));
                    break;
                }
            }
        }

        Elements table = doc.getElementsByTag("table");
        Elements div = doc.getElementsByTag("div");

        //页眉表格
        if (!table.isEmpty()) {
            Element node = table.get(table.size() - 1);
            String text = node.text();

            if (text.equals(header)) {
                //把页眉替换到头部
                Element child = div.first().child(0);
                child.before(node.toString());
                //删除多余页眉
                node.remove();
            }

        }
        //页眉非表格
        if (null != header) {
            // TODO 暂时没有好的解决办法，后期加
        }

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

            if (href.isEmpty()) {
                href = "http://localhost/test/wordDoc/?word=" + anode.text().split("》")[0]; //修改style中的url值
            }

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
            File file = new File("F:/1.html");
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
