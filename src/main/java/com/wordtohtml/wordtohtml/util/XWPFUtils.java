package com.wordtohtml.wordtohtml.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

import com.microsoft.schemas.vml.CTShape;

import javax.servlet.http.HttpServletRequest;

/**
 * 获取2007word中的图片索引（例：Ird4,Ird5）
 * @author zcy
 *
 */
public class XWPFUtils {

    //获取某一个段落中的所有图片索引
    public static List<String> readImageInParagraph(XWPFParagraph paragraph) {
        //图片索引List
        List<String> imageBundleList = new ArrayList<String>();

        //段落中所有XWPFRun
        List<XWPFRun> runList = paragraph.getRuns();
        for (XWPFRun run : runList) {
            //XWPFRun是POI对xml元素解析后生成的自己的属性，无法通过xml解析，需要先转化成CTR
            CTR ctr = run.getCTR();
            //对子元素进行遍历
            XmlCursor c = ctr.newCursor();
            //这个就是拿到所有的子元素：
            c.selectPath("./*");
            while (c.toNextSelection()) {
                XmlObject o = c.getObject();
                //如果子元素是<w:drawing>这样的形式，使用CTDrawing保存图片
                if (o instanceof CTDrawing) {
                    CTDrawing drawing = (CTDrawing) o;
                    CTInline[] ctInlines = drawing.getInlineArray();
                    for (CTInline ctInline : ctInlines) {
                        CTGraphicalObject graphic = ctInline.getGraphic();
                        //
                        XmlCursor cursor = graphic.getGraphicData().newCursor();
                        cursor.selectPath("./*");
                        while (cursor.toNextSelection()) {
                            XmlObject xmlObject = cursor.getObject(); 
                            // 如果子元素是<pic:pic>这样的形式
                            if (xmlObject instanceof CTPicture) {
                                org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture picture = (org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture) xmlObject;
                                //拿到元素的属性
                                imageBundleList.add(picture.getBlipFill().getBlip().getEmbed());
                            }
                        }
                    }
                }
                //使用CTObject保存图片
                //<w:object>形式
                if (o instanceof CTObject) {
                    CTObject object = (CTObject) o;
                    XmlCursor w = object.newCursor();
                    w.selectPath("./*");
                    while (w.toNextSelection()) {
                        XmlObject xmlObject = w.getObject();
                        if (xmlObject instanceof CTShape) {
                            CTShape shape = (CTShape) xmlObject;
                            /*imageBundleList.add(shape.getImagedataArray()[0].getId2());*/
                        }
                    }
                }
            }
        }
        return imageBundleList;
    }

    /**
     * 获取word中图片上传到文件服务器
     * @return
     * @throws
     */
    public static  List<Map<String,String>> getImgUrl(String url) throws Exception{
        /* * 实现思路
         * 1、根据段落docx获取图片索引
         * 2、根据获取到的图片数据标识，在总的docx中获取图片data数据
         * 3.上传图片返回访问路径；*/
        //未分割之前的总文件地址
//        ResourceBundle resource = ResourceBundle.getBundle("URL");
//        String imgLocalPath = resource.getString("imgLocalPath");
//        String Indexdocx =request.getSession().getAttribute("wordRootPath").toString();
        //读取总文件
        InputStream in = new FileInputStream(url);
        XWPFDocument xwpfDocumentIndex = new XWPFDocument(in);
        in.close();
        List<XWPFPictureData> list = xwpfDocumentIndex.getAllPackagePictures();
        //需要获取数据的图片名称
        String paraPicName = "";
        //总文档中的图片名称
        String pictureName ="";
        //上传到图片服务器之后的图片名称
        //图片索引rId1/rId2/rId3..
        String id ="";
        String uuidName = "";
        String endName = "";
        byte[] bd = null;
        //方法返回的List包含，题目序号，上传之后图片名称
        List<Map<String,String>> resMapList = new ArrayList<Map<String,String>>();
        Map<String, String> imgUploadNameMap = new HashMap<String,String>();
        for (XWPFPictureData xwpfPictureData : list) {
            uuidName = UUID.randomUUID().toString();
            id = xwpfPictureData.getParent().getRelationId(xwpfPictureData);
            pictureName = xwpfPictureData.getFileName();
            endName = pictureName.substring(pictureName.lastIndexOf("."));
            bd = xwpfPictureData.getData();
            FileOutputStream fos = new FileOutputStream(new File("F:/test.png"));
            fos.write(bd);
            fos.flush();
            fos.close();
            //ImageSizer.imageZip(new File(imgLocalPath+uuidName+endName), new File(imgLocalPath+uuidName+"-e"+endName), "", 130, 130, 1);
            imgUploadNameMap.put(id, uuidName+endName);
        }
        //遍历参数
        String tempPicName = "";
        String tempValue ="";
//        for (Map<String, String> map : imgMsgList) {
//            tempPicName = map.get("pictureName");
//            tempValue = imgUploadNameMap.get(tempPicName);
//            if(tempValue!=null){
//                map.put("pictureName", tempValue);
//            }else{
//                map.put("pictureName", "");
//            }
//            resMapList.add(map);
//        }
        return resMapList;
    }

}
