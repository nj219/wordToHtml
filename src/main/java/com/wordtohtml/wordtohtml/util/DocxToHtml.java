package com.wordtohtml.wordtohtml.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.entity.ContentType;
import org.apache.poi.poifs.property.Child;
import org.apache.poi.xwpf.converter.core.BasicURIResolver;
import org.apache.poi.xwpf.converter.core.FileImageExtractor;
import org.apache.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHdrFtr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName docxToHtmlController
 * @Author 牛杰
 * @Date 2019/8/5 15:33
 **/
@Component
@Slf4j
public class DocxToHtml {

    private static String url;

    @Value("${upload.interface}")
    public void setUrl(String url) {
        DocxToHtml.url = url;
    }

    private static String uploadUrl;

    @Value("${uploadUrl}")
    public void setUploadUrl(String uploadUrl) {
        DocxToHtml.uploadUrl = uploadUrl;
    }

    /**
     * 解析docx
     *
     * @param fileName    文件名称
     * @param fileOutName 输出路径
     * @throws TransformerException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public static String docx2Html(String fileName, String fileOutName) throws IOException {
        //long startTime = System.currentTimeMillis();
        XWPFDocument document = new XWPFDocument(new FileInputStream(fileName));

        //解析正文xml
        CTDocument1 documentXml = document.getDocument();
        String bodyHtml = parsingBodyXML(documentXml);

        //页眉页脚
        XWPFHeaderFooterPolicy headerFooterPolicy = document.getHeaderFooterPolicy();
        XWPFHeader header = headerFooterPolicy.getDefaultHeader();  //页眉
        XWPFFooter footer = headerFooterPolicy.getDefaultFooter();  //页脚

        //导出页眉图片
        List<XWPFPictureData> allPictures = header.getAllPictures();
        int size = allPictures.size();
        String upUrl = "";
        if (size >0) {
            for (XWPFPictureData picture : allPictures) {
                String picName = picture.getFileName();
                try {
                    String type = picName.split("[.]")[1];
                    byte[] data = picture.getData();
                    InputStream inputStream = new ByteArrayInputStream(data);
                    MultipartFile file = new MockMultipartFile("new." + type,"old." + type, ContentType.APPLICATION_OCTET_STREAM.toString(), inputStream);

                    upUrl = HttpClientUtils.httpClientUploadFile(url, file);
                } catch (Exception e) {
                    log.info("------------------------------页眉图片解析异常-------------------------------------------");
                }

                Map<String, String> parse = (Map<String, String>) JSONObject.parse(upUrl);
                for (String value : parse.values()) {
                    upUrl = value;
                    break;
                }

                log.info(upUrl);
            }
        }

        //页眉表格
        Document parse = null;
        String headerHtml = "";
        List<XWPFTable> headerTables = header.getTables();
        if (headerTables.size() <= 0) {
            //页眉非表格
            CTHdrFtr headerXml = header._getHdrFtr();
            headerHtml = headerParsing(headerXml, upUrl);
        }
        //页眉表格
        for (XWPFTable table : headerTables) {
            //页眉解析
            CTTbl ctTbl = table.getCTTbl();    //xml格式

            //解析xml后返回的HTML数据
            String form = headerTableXML(ctTbl);

            //解析HTML替换某些参数
            parse = Jsoup.parse(form);
            Elements div = parse.select("div");

            div.get(2).attr("style", "display: inline-block;border-right:1px solid #000;padding-right: 50px");
            String text = div.get(3).text();
            div.get(3).attr("style", "display: inline-block;text-align: right;float: right");
            div.get(2).appendElement("img style=\"width:1.8988042in;height:0.4295625in;vertical-align:text-bottom;\" src=\"" + uploadUrl + upUrl + "\"");

            Elements span = parse.select("span");
            //span.get(1).attr("style", "text-align: right;font-family: 宋体;font-weight: bold;");
            //span.get(0).attr("style", "text-align: right;font-family: Times New Roman;font-weight: bold;");
            /*for (Element pan : span) {
                if (pan.text().equals("□")) {
                    pan.attr("style", "font-family: 宋体;float: left; display: block;");
                }
            }*/

            //提取body
            Elements body = parse.getElementsByTag("body");
            headerHtml = body.get(0).children().toString();
        }


        //页脚
        CTHdrFtr ctHdrFtr = footer._getHdrFtr();  //页脚xml
        String footerHtml = footerParsing(ctHdrFtr);

        return null;
    }

    /**
     * 解析正文xml
     * @param documentXml
     * @return
     */
    private static String parsingBodyXML(CTDocument1 documentXml) {
        String bodyHtml = "";

        Document bodyDocument = Jsoup.parse(documentXml.toString());
        //解析最大节点
        //TODO 图片未解析
        Elements wBody = bodyDocument.getElementsByTag("w:body");
        for (Element body : wBody) {
            //解析每一个子元素
            //bodyHtml += "<div>";
            Elements children = body.children();
            for (Element child : children) {
                //获取当前节点的节点名称
                String tagName = child.tagName();

                //和几个特定的节点名称作对比，拼接不同HTML标签
                //一般是字儿
                if ("w:p".equals(tagName)) {
                    String textAlign = "";
                    bodyHtml += "<div>";
                    Elements wppr = child.getElementsByTag("w:pPr");
                    Elements wr = child.getElementsByTag("w:r");
                    //获取样式
                    for (Element ppr : wppr) {
                        Elements wjc = ppr.getElementsByTag("w:jc");
                        for (Element jc : wjc) {
                            textAlign = "text-align: " + jc.attr("w:val");
                        }
                    }

                    bodyHtml += "<p style='"+textAlign+"'>";
                    //获取文本内容
                    for (Element r : wr) {
                        String font = "";
                        String eleWb = "";
                        String fontcolor = "";

                        //字体样式
                        Elements wrpr = r.getElementsByTag("w:rPr");
                        for (Element rpr : wrpr) {
                            //字体
                            Elements wrFonts = rpr.getElementsByTag("w:rFonts");
                            for (Element wrFont : wrFonts) {
                                font = "font: \"" + wrFont.attr("w:hint") + "\";";
                            }
                            //加粗
                            Elements wb = rpr.getElementsByTag("w:b");
                            if (!wb.isEmpty()) {
                                eleWb = "font-weight: bold;";
                            }
                            //颜色
                            Elements wcolor = rpr.getElementsByTag("w:color");
                            for (Element color : wcolor) {
                                fontcolor = "color: #" + color.attr("w:val") + ";";
                            }

                        }

                        //文本
                        Elements wt = r.getElementsByTag("w:t");
                        if (wt.isEmpty()) {
                            bodyHtml += "<br />";
                        }
                        bodyHtml += "<span style='" + font + eleWb + fontcolor + "'>";
                        for (Element t : wt) {
                            String space = t.attr("xml:space");
                            if (!space.isEmpty()) {
                                bodyHtml += "&nbsp;";
                            }
                            bodyHtml += t.text();
                        }

                        //图片
                        Elements wdrawing = r.getElementsByTag("w:drawing");
                        for (Element drawind : wdrawing) {

                        }

                        bodyHtml += "</span>";
                    }

                    bodyHtml += "</p>";
                    bodyHtml += "</div>";
                }

                //一般为表格
                if ("w:tbl".equals(tagName)) {
                    //字体
                    String font = "";

                    bodyHtml += "<table border cellspacing=\"0\" cellpadding=\"0\">";
                    //解析行
                    Elements wtr = child.getElementsByTag("w:tr");
                    for (Element tr : wtr) {
                        bodyHtml += "<tr>";

                        Elements wtc = tr.getElementsByTag("w:tc");
                        for (Element tc : wtc) {
                            bodyHtml += "<td>";

                            Elements wp = tc.getElementsByTag("w:p");
                            for (Element p : wp) {
                                Elements wr = p.getElementsByTag("w:r");

                                //如果节点为空换行
                                if (wr.isEmpty()) {
                                    bodyHtml += "<br />";
                                }

                                for (Element r : wr) {
                                    //样式
                                    Elements wrpr = r.getElementsByTag("w:rPr");
                                    for (Element rpr : wrpr) {
                                        //字体
                                        Elements wrfonts = rpr.getElementsByTag("w:rFonts");
                                        for (Element wrfont : wrfonts) {
                                            font = "font: \"" + wrfont.attr("w:hint") + "\";";
                                        }
                                    }

                                    //文字
                                    bodyHtml += "<span style='" + font + "'>";
                                    Elements wt = r.getElementsByTag("w:t");
                                    for (Element t : wt) {
                                        bodyHtml += t.text();

                                    }
                                    bodyHtml += "</span>";
                                }
                            }

                            bodyHtml += "</td>";
                        }

                        bodyHtml += "</tr>";
                    }

                    bodyHtml += "</table>";
                }

            }
        }

        return bodyHtml;
    }

    /**
     * 解析页脚xml
     * @param footer
     * @return
     */
    private static String footerParsing(CTHdrFtr footer) {
        String footerHtml = "";
        org.jsoup.nodes.Document doc = Jsoup.parse(footer.toString());

        Elements wp = doc.getElementsByTag("w:p");

        footerHtml += "<p>";
        for (Element p : wp) {
            Elements wr = p.getElementsByTag("w:r");
            for (Element r : wr) {
                Elements wrpr = r.getElementsByTag("w:rPr");
                Elements wt = r.getElementsByTag("w:t");

                String font = "";
                String color = "";
                boolean pageNum = false;
                for (Element rpr : wrpr) {
                    //页码不解析
                    Elements wrstyle = rpr.getElementsByTag("w:rStyle");
                    for (Element rstyle : wrstyle) {
                        String page = rstyle.attr("w:val");
                        if ("PageNumber".equals(page)) {
                            pageNum = true;
                        }
                    }

                    //字体
                    Elements wrfonts = rpr.getElementsByTag("w:rFonts");
                    for (Element wrfont : wrfonts) {
                        font = wrfont.attr("w:hAnsi");
                    }

                    //颜色
                    Elements wcolors = rpr.getElementsByTag("w:color");
                    for (Element wcolor : wcolors) {
                        color = wcolor.attr("w:val");
                    }

                }

                for (Element t : wt) {
                    if (pageNum) {
                        continue;
                    }
                    footerHtml += "<span style='font-family: " + font + ";color: #" + color + "'>" + t.text() + "</span>";
                }
            }
            footerHtml += "<br />";
        }
        footerHtml += "</p>";
        return footerHtml;
    }

    /**
     * 解析页眉xml(表格)
     * @param header
     * @return
     */
    private static String headerTableXML(CTTbl header) {
        String heaerHtml = "";
        org.jsoup.nodes.Document doc = Jsoup.parse(header.toString());

        Elements wtr = doc.getElementsByTag("w:tr");

        ArrayList<Integer> tList = new ArrayList();
        int total = 0;

        heaerHtml += "<div style=\"margin: 0 auto; display:inline-block;\">";
        for (Element tr : wtr) {
            heaerHtml += "<div style=\"border:1px solid #000\">";
            Elements wtc = tr.getElementsByTag("w:tc");
            for (Element tc : wtc) {
                heaerHtml += "<div style=\"display: inline-block;border-left:1px solid #000;padding-right: 50px\">";
                Elements wp = tc.getElementsByTag("w:p");
                for (Element p : wp) {
                    Elements wr = p.getElementsByTag("w:r");
                    Elements wppr = p.getElementsByTag("w:pPr");

                    String attrwjc = "";
                    boolean br = false;
                    for (Element ppr : wppr) {
                        Elements wjc = ppr.getElementsByTag("w:jc");
                        Elements wrpr = ppr.getElementsByTag("w:rPr");

                        if (!wrpr.isEmpty()) {
                            br = true;
                        }

                        if (!wjc.isEmpty()) {
                            for (Element jc : wjc) {
                                attrwjc = jc.attr("w:val");
                            }
                        }
                    }

                    for (Element r : wr) {
                        Elements wt = r.getElementsByTag("w:t");
                        Elements wrpr = r.getElementsByTag("w:rPr");

                        String attrfont = "";
                        String eleWb = "";
                        String eleWbcs = "";
                        //字体样式
                        for (Element rpr : wrpr) {
                            Elements wrfont = rpr.getElementsByTag("w:rFonts");
                            Elements wb = rpr.getElementsByTag("w:b");
                            Elements wbcs = rpr.getElementsByTag("w:bCs");

                            for (Element rfont : wrfont) {
                                attrfont = "font-family: " + rfont.attr("w:hAnsi") + ";";
                            }

                            if (!wb.isEmpty()) {
                                eleWb = "font-weight: bold;";
                            }

                            /*if (!wbcs.isEmpty()) {
                                eleWbcs = "<br />";
                            }*/
                        }
                        //文字
                        for (Element t : wt) {
                            if (attrwjc != "") {
                                heaerHtml += eleWbcs + "<span style='float: left; display: block; text-align: "+attrwjc + ";" + attrfont + eleWb + "'>" + t.text() + "</span>";
                            } else {
                                heaerHtml += eleWbcs + "<span style=\"float: left; display: block;" + attrfont + eleWb + "\">" + t.text() + "</span>";
                            }

                        }
                    }

                    //换行，后期必须要加的时候打开，可能兼容性不是特别好
                    /*if (br) {
                        heaerHtml += "<br />";
                    }*/

                }
                heaerHtml += "</div>";
            }
            heaerHtml += "</div>";
        }
        heaerHtml += "</div>";
        return heaerHtml;
    }

    /**
     * 解析页眉xml(非表格)
     * @param headerXml
     * @return
     */
    private static String headerParsing(CTHdrFtr headerXml, String url) {
        String headerHtml = "";

        //解析根节点
        Document doc = Jsoup.parse(headerXml.toString());
        headerHtml += "<p>";

        Elements wr = doc.getElementsByTag("w:r");
        for (Element r : wr) {
            String font = "";

            //解析样式
            Elements wrpr = r.getElementsByTag("w:rPr");
            for (Element rpr : wrpr) {
                //字体
                Elements wrfonts = rpr.getElementsByTag("w:rFonts");
                for (Element wrfont : wrfonts) {
                    font = "font: '" + wrfont.attr("w:hAnsi") + "';";
                }
            }

            //文字
            Elements wt = r.getElementsByTag("w:t");
            headerHtml += "<span style='" + font + "'>";
            for (Element t : wt) {
                if (t.text().isEmpty()) {
                    String space = t.attr("xml:space");
                    if (!space.isEmpty()) {
                        headerHtml += "&nbsp;";
                    }
                }

                String space = t.attr("xml:space");
                if (!space.isEmpty()) {
                    headerHtml += "&nbsp;";
                }

                headerHtml += t.text();
            }
            headerHtml += "</span>";

            //图片
            Elements wdrawing = r.getElementsByTag("w:drawing");
            if (!wdrawing.isEmpty()) {
                headerHtml += "<img width=\"150px\" height=\"35px\" src=" + uploadUrl + url + " />";
            }
        }

        headerHtml += "</p>";

        return headerHtml;
    }
}