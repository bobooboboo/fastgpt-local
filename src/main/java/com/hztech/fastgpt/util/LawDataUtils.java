package com.hztech.fastgpt.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.mutable.MutableObj;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hztech.fastgpt.dao.po.LawContentDO;
import com.hztech.fastgpt.dao.po.LawDO;
import com.hztech.fastgpt.model.dto.response.CountryLawBasicDataResponseDTO;
import com.hztech.fastgpt.model.dto.response.CountryLawDetailDataResponseDTO;
import com.hztech.fastgpt.model.dto.response.LawDetailResponseDTO;
import com.hztech.fastgpt.model.dto.response.LawResponseDTO;
import com.hztech.fastgpt.model.enums.EnumLawContentType;
import com.hztech.fastgpt.model.enums.EnumLawSource;
import com.hztech.fastgpt.model.enums.EnumLawStatus;
import com.hztech.fastgpt.model.enums.EnumLawType;
import com.hztech.fastgpt.service.ILawContentService;
import com.hztech.fastgpt.service.ILawService;
import com.hztech.util.*;
import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import com.spire.doc.Section;
import com.spire.doc.documents.Paragraph;
import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 法律法规数据工具类
 *
 * @author: boboo
 * @Date: 2024/2/1 11:59
 **/
@Slf4j
public class LawDataUtils {

    /**
     * 国家法律法规数据库详情页请求URL
     */
    private static final String DETAIL_DATA_URL = "https://flk.npc.gov.cn/api/detail";

    /**
     * 国家法律法规数据库详情页附件URL前缀
     */
    private static final String FILE_URL = "https://wb.flk.npc.gov.cn";

    /**
     * 国家法律法规数据库列表页请求URL
     */
    private static final Map<EnumLawType, List<String>> BASIC_DATA_URL_MAP = new LinkedHashMap<>(8);

    static {
        // 宪法
//        BASIC_DATA_URL_MAP.put(EnumLawType.CONSTITUTION, Collections.singletonList("https://flk.npc.gov.cn/api/?page=%s&size=%s&xlwj=01"));
        // 法律
//        BASIC_DATA_URL_MAP.put(EnumLawType.STATUTE, Collections.singletonList("https://flk.npc.gov.cn/api/?page=%s&size=%s&type=flfg"));
        // 行政法规
//        BASIC_DATA_URL_MAP.put(EnumLawType.ADMINISTRATIVE_REGULATIONS, Collections.singletonList("https://flk.npc.gov.cn/api/?page=%s&size=%s&type=xzfg"));
        // 监察法规
//        BASIC_DATA_URL_MAP.put(EnumLawType.SUPERVISION_REGULATIONS, Collections.singletonList("https://flk.npc.gov.cn/api/?page=%s&size=%s&type=jcfg"));
        // 司法解释
//        BASIC_DATA_URL_MAP.put(EnumLawType.JUDICIAL_INTERPRETATION, Collections.singletonList("https://flk.npc.gov.cn/api/?page=%s&size=%s&type=sfjs"));
        // 地方性法规
        BASIC_DATA_URL_MAP.put(EnumLawType.LOCAL_REGULATIONS, Collections.singletonList("https://flk.npc.gov.cn/api/?page=%s&size=%s&type=dfxfg"));
    }

    /**
     * 国家法律法规数据库时效性关系映射
     */
    private static final Map<String, EnumLawStatus> LAW_STATUS_MAP = new LinkedHashMap<>(8);

    static {
        // 已废止
        LAW_STATUS_MAP.put("9", EnumLawStatus.REPEALED);
        // 生效
        LAW_STATUS_MAP.put("1", EnumLawStatus.EFFECTIVE);
        // 已修改
        LAW_STATUS_MAP.put("5", EnumLawStatus.MODIFIED);
        // 未生效
        LAW_STATUS_MAP.put("3", EnumLawStatus.NOT_EFFECTIVE);
        // 空
        LAW_STATUS_MAP.put("7", EnumLawStatus.NONE);
    }

    /**
     * 汉字章节编号
     */
    private static final String NUM_STR = "零一二三四五六七八九十百千";

    /**
     * 编正则
     */
    private static final String PART_REGEX = String.format("^\\s*第([%s]+)[编]+.*", NUM_STR);

    /**
     * 章正则
     */
    private static final String CHAPTER_REGEX = String.format("^\\s*第([%s]+)[章]+.*", NUM_STR);

    /**
     * 节正则
     */
    private static final String SECTION_REGEX = String.format("^\\s*第([%s]+)[节]+.*", NUM_STR);

    /**
     * 条正则
     */
    private static final String ARTICLE_REGEX = String.format("^(\\s|\u3000|\u00A0)*第([%s]+)条.*", NUM_STR);

    /**
     * 目录正则
     */
    public static final String CATALOGUE_REGEX = "^\\s*(目(\\s|\u3000|\u00A0)*录)\\s*";

    /**
     * 序言正则
     */
    public static final String FOREWORD_REGEX = "^\\s*(序(\\s|\u3000|\u00A0)*言)\\s*";

//    /**
//     * 地区正则
//     */
//    private static final String AREA_REGEX = "(?<province>[^省]+自治区|.*?省|.*?行政区|.*?市)?(?<city>[^市]+自治州|.*?地区|.*?行政单位|.+盟|市辖区|.*?市|.*?县)?(?<county>[^县]+县|.+区|.+市|.+旗|.+海域|.+岛)?";

    /**
     * 编正则Pattern
     */
    public static final Pattern PART_PATTERN = Pattern.compile(PART_REGEX);

    /**
     * 章节正则Pattern
     */
    public static final Pattern CHAPTER_PATTERN = Pattern.compile(CHAPTER_REGEX);

    /**
     * 节正则Pattern
     */
    public static final Pattern SECTION_PATTERN = Pattern.compile(SECTION_REGEX);

    /**
     * 条正则Pattern
     */
    public static final Pattern ARTICLE_PATTERN = Pattern.compile(ARTICLE_REGEX);

//    /**
//     * 地区正则Pattern
//     */
//    private static final Pattern AREA_PATTERN = Pattern.compile(AREA_REGEX);

    /**
     * cookie
     */
    @Getter
    private static String cookieStr;

    public static void setCookieStr(String cookieStr) {
        LawDataUtils.cookieStr = cookieStr;
    }

    /**
     * 远程获取国家法律法规数据
     */
    public static void fetchData(Integer page, Integer stop) {
        List<CountryLawBasicDataResponseDTO> basicDataList = fetchBasic(page, stop);
        FileUtil.writeUtf8String(JSONUtil.toJsonStr(basicDataList), "/root/lawPage.txt");
    }

    public static void build(List<CountryLawBasicDataResponseDTO> basicDataList) {
        for (CountryLawBasicDataResponseDTO basicData : basicDataList) {
            MutableObj<LawResponseDTO> mutableObj = new MutableObj<>();
            mutableObj.set(fetchDetail(basicData.getId()));
            LawResponseDTO responseDTO = mutableObj.get();
            if (HzCollectionUtils.isNotEmpty(responseDTO.getDetails())) {
                String docFileUrl = responseDTO.getDocFileUrl();
                String pdfFileUrl = responseDTO.getPdfFileUrl();

                LawDO lawStatisticsDO = new LawDO();
                lawStatisticsDO.setType(basicData.getLegislationType());
                lawStatisticsDO.setOuterId(basicData.getId());
                lawStatisticsDO.setTitle(basicData.getTitle());
                lawStatisticsDO.setSubject(basicData.getOffice());
                lawStatisticsDO.setEffective(basicData.getExpiry());
                lawStatisticsDO.setPublish(basicData.getPublish());
                lawStatisticsDO.setDataSource(EnumLawSource.NATIONAL_LAWS_AND_REGULATIONS_DATABASE);
                lawStatisticsDO.setDocFileUrl(docFileUrl);
                lawStatisticsDO.setPdfFileUrl(pdfFileUrl);
                lawStatisticsDO.setStatus(LAW_STATUS_MAP.get(basicData.getStatus()));

                boolean saved = HzSpringUtils.getBean(ILawService.class).save(lawStatisticsDO);
                if (saved) {
                    List<LawContentDO> list = responseDTO.getDetails().stream().map(lawDetailResponseDTO -> {
                        LawContentDO lawElasticSearchDO = new LawContentDO();
                        lawElasticSearchDO.setPart(lawDetailResponseDTO.getPart());
                        lawElasticSearchDO.setChapter(lawDetailResponseDTO.getChapter());
                        lawElasticSearchDO.setSection(lawDetailResponseDTO.getSection());
                        lawElasticSearchDO.setArticle(lawDetailResponseDTO.getArticle());
                        lawElasticSearchDO.setContentType(lawDetailResponseDTO.getContentType());
                        lawElasticSearchDO.setDocFileUrl(docFileUrl);
                        lawElasticSearchDO.setPdfFileUrl(pdfFileUrl);
                        lawElasticSearchDO.setOuterId(basicData.getId());
                        lawElasticSearchDO.setType(basicData.getLegislationType());
                        lawElasticSearchDO.setStatus(LAW_STATUS_MAP.get(basicData.getStatus()));
                        lawElasticSearchDO.setTitle(basicData.getTitle());
                        lawElasticSearchDO.setSubject(basicData.getOffice());
                        lawElasticSearchDO.setEffective(basicData.getExpiry());
                        lawElasticSearchDO.setPublish(basicData.getPublish());
                        lawElasticSearchDO.setContent(lawDetailResponseDTO.getContent());
                        lawElasticSearchDO.setDataSource(EnumLawSource.NATIONAL_LAWS_AND_REGULATIONS_DATABASE);
                        return lawElasticSearchDO;
                    }).collect(Collectors.toList());
                    HzSpringUtils.getBean(ILawContentService.class).insertBatch(list);
                }
            }
        }

    }

    /**
     * 获取列表页基础数据
     */
    private static List<CountryLawBasicDataResponseDTO> fetchBasic(Integer page, Integer stop) {
        List<CountryLawBasicDataResponseDTO> list = new ArrayList<>(22596);
        BASIC_DATA_URL_MAP.forEach((enumLawType, urls) -> {
            for (String url : urls) {
                log.info("开始爬取{}的数据", enumLawType.getDesc());
                AtomicInteger startPage = new AtomicInteger(page);
                int size = 10;
                AtomicBoolean hasMore = new AtomicBoolean(true);
                while (hasMore.get() && startPage.get() <= stop) {
                    MDC.put("page", startPage.get() + "");
                    RetryUtils.tryExecute(-1, () -> {
                        log.info("开始爬取{}的第{}页数据", enumLawType.getDesc(), startPage);
                        HttpRequest httpRequest = HttpRequest.post(String.format(url, startPage, size)).setFollowRedirects(true);
                        @Cleanup
                        HttpResponse response = safeExecute(httpRequest);
                        String body = response.body();
//                        log.info("{}的第{}页数据返回 ==> {}", enumLawType.getDesc(), startPage, body);
                        if (JSONUtil.isTypeJSON(body)) {
                            JSONObject result = JSONUtil.parseObj(body).getJSONObject("result");
                            hasMore.set(startPage.getAndIncrement() * size < result.getInt("totalSizes"));
                            List<CountryLawBasicDataResponseDTO> basicDataList = result.getJSONArray("data").toList(CountryLawBasicDataResponseDTO.class);
                            basicDataList.forEach(basicData -> basicData.setLegislationType(HzEnumUtils.fromDesc(EnumLawType.class, basicData.getType())));
                            list.addAll(basicDataList);
                            build(basicDataList);
                        } else {
                            throw new RuntimeException("返回数据不是json");
                        }
                    }, "法规列表");
                }
            }
        });
        return list;
    }

    /**
     * 获取法律法规详情
     *
     * @param id 法律法规数据id
     */
    private static LawResponseDTO fetchDetail(String id) {
        LawResponseDTO responseDTO = new LawResponseDTO();
        log.info("开始请求第{}页id:{}的详情", MDC.get("page"), id);
        MutableObj<List<CountryLawDetailDataResponseDTO>> list = new MutableObj<>();
        RetryUtils.tryExecute(-1, () -> {
            HttpRequest httpRequest = HttpRequest.post(DETAIL_DATA_URL).contentType(ContentType.FORM_URLENCODED.toString()).form("id", id);
            try (HttpResponse response = safeExecute(httpRequest)) {
                String body = response.body();
//            log.info("id:{}的details ==> {}", id, body);
                JSONObject result = JSONUtil.parseObj(body).getJSONObject("result");
                list.set(result.getJSONArray("body").toList(CountryLawDetailDataResponseDTO.class));
            } catch (Exception e) {
                log.error("id:{}获取details异常", id);
//                log.error("id:{}获取details异常 ==> {}", id, ExceptionUtil.stacktraceToString(e));
//            log.error("httpRequest对象 ==> {}", httpRequest);
                throw e;
            }
            upload(list.get(), responseDTO);
            responseDTO.setDetails(read(list.get()));
        }, "法规详情");
        return responseDTO;
    }


    /**
     * 读取内容
     * 读取顺序 WORD > HTML > PDF
     */
    public static List<LawDetailResponseDTO> read(List<CountryLawDetailDataResponseDTO> list) {
        Optional<CountryLawDetailDataResponseDTO> optional = list.stream().filter(detailData -> HzStringUtils.equals(detailData.getType(), "WORD", true)).findFirst();
        if (optional.isPresent()) {
            CountryLawDetailDataResponseDTO detailData = optional.get();
            return readFromDocx(detailData.getPath(), FileFormat.Docx);
        } else if ((optional = list.stream().filter(detailData -> HzStringUtils.equals(detailData.getType(), "HTML", true)).findFirst()).isPresent()) {
            CountryLawDetailDataResponseDTO detailData = optional.get();
            return readFromDocx(detailData.getUrl(), FileFormat.Html);
        } else if ((optional = list.stream().filter(detailData -> HzStringUtils.equals(detailData.getType(), "PDF", true)).findFirst()).isPresent()) {
//            CountryLawDetailDataResponseDTO detailData = optional.get();
//            String path = detailData.getPath();
//            return readFromPdf(detailData.getPath());
//            throw new HzRuntimeException("暂时无法从PDF中读取法律法规内容,path=" + path);
        }
        return Collections.emptyList();
    }

    /**
     * 上传附件
     */
    public static void upload(List<CountryLawDetailDataResponseDTO> list, LawResponseDTO responseDTO) {
        String docPath = null, pdfPath = null;
        Optional<CountryLawDetailDataResponseDTO> optional = list.stream().filter(detailData -> HzStringUtils.equals(detailData.getType(), "PDF", true)).findFirst();
        if (optional.isPresent()) {
            CountryLawDetailDataResponseDTO detailData = optional.get();
            pdfPath = detailData.getPath();
        }
        if ((optional = list.stream().filter(detailData -> HzStringUtils.equals(detailData.getType(), "WORD", true)).findFirst()).isPresent()) {
            CountryLawDetailDataResponseDTO detailData = optional.get();
            docPath = detailData.getPath();
        } else if ((optional = list.stream().filter(detailData -> HzStringUtils.equals(detailData.getType(), "HTML", true)).findFirst()).isPresent()) {
            CountryLawDetailDataResponseDTO detailData = optional.get();
            docPath = detailData.getUrl();
        }
        boolean hasWps = HzStringUtils.isNotBlank(docPath);
        boolean hasPdf = HzStringUtils.isNotBlank(pdfPath);
        if (hasWps && hasPdf) {
            // wps和pdf文件都有 wps转换为pdf
            saveFile(docPath, true, responseDTO);
            saveFile(pdfPath, false, responseDTO);
        } else if (hasWps) {
            // 只有wps文件 转换为pdf
            saveFile(docPath, true, responseDTO);
        } else {
            // 只有pdf文件 无需转换
            saveFile(pdfPath, false, responseDTO);
        }
    }

    private static void saveFile(String path, boolean isDoc, LawResponseDTO responseDTO) {
        String fileUrl = "/fstore/" + HzFileUtils.getName(path);
        File file = FileUtil.writeBytes(getFileBytes(path), fileUrl);
        if (file != null) {
            if (isDoc) {
                responseDTO.setDocFileUrl(fileUrl);
            } else {
                responseDTO.setPdfFileUrl(fileUrl);
            }
        }
    }

//    public static void main(String[] args) {
////        String str = "{\"timestamp\":1709194029638,\"success\":true,\"message\":\"success\",\"code\":200,\"result\":{\"title\":\"芜湖市生活垃圾分类管理条例\",\"office\":\"芜湖市人民代表大会常务委员会\",\"publish\":\"2024-01-15 00:00:00\",\"expiry\":\"2024-06-01 00:00:00\",\"status\":\"3\",\"level\":\"地方性法规\",\"body\":[{\"type\":\"WORD\",\"path\":\"/dfxfg/WORD/ab1b248f294841c0b0d02e7c2719aa9d.docx\",\"addr\":\"ff8081818c3ce31f018d3918b1190667\",\"url\":\"/dfxfg/html/c028f0e09f87afab7a940336086c6f07522ec64f.html\",\"mobile\":\"/dfxfg/html/c028f0e09f87afab7a940336086c6f07522ec64f-mobile.html\",\"qr\":\"/dfxfg/PNG/ab1b248f294841c0b0d02e7c2719aa9d.png\"}],\"dec\":null,\"bbbs\":null,\"otherFile\":null}}";
////        JSONObject jsonObject = JSONUtil.parseObj(str);
////        JSONObject result = jsonObject.getJSONObject("result");
////        JSONArray otherFile = jsonObject.getJSONArray("otherFile");
////        System.out.println(1);
////        Document document = new Document(getFileStream("/xffl/WORD/9f314b7b2f2b4a66b63e8751da3fde8f.docx"), FileFormat.Docx);
////        readFromDocxWithArticlePattern(document);
////        System.out.println(1);
////        Document document = new Document("C:\\Users\\PC_Admin\\Desktop\\1.docx", FileFormat.Docx);
////        for (int i = 0; i < document.getSections().getCount(); i++) {
////            Section section = document.getSections().get(i);
////            for (int j = 0; j < section.getParagraphs().getCount(); j++) {
////                Paragraph paragraph = section.getParagraphs().get(j);
////                for (int k = 0; k < paragraph.getChildObjects().getCount(); k++) {
////                    DocumentObject documentObject = paragraph.getChildObjects().get(k);
////                    DocumentObjectType documentObjectType = documentObject.getDocumentObjectType();
////                    System.out.println("在第" + (j + 1) + "段的第" + (k + 1) + "部分类型为：" + documentObjectType);
////                    if (documentObjectType == DocumentObjectType.Picture) {
////                        // 找到了图片
////                        DocPicture picture = (DocPicture) documentObject;
////                        picture.getImage();
////                        System.out.println("在第" + (j + 1) + "段的第" + (k + 1) + "部分找到了图片");
////                    } else if (documentObjectType == DocumentObjectType.Text_Range) {
////                        TextRange textRange = (TextRange) documentObject;
////                        String text = textRange.getText();
////                        System.out.println("在第" + (j + 1) + "段的第" + (k + 1) + "部分找到了文字：" + text);
////                    }
////                }
////            }
////        }
//        fetchData();
//    }

    private static boolean isBlank(String str) {
        return HzStringUtils.isBlank(str) || StrUtil.equalsAny(str, "\uFEFF", "\u200B");
    }

    /**
     * 读取正常格式的docx
     *
     * @param document docx文档
     */
    @SuppressWarnings("all")
    private static List<LawDetailResponseDTO> readFromDocxWithArticlePattern(Document document) {
        List<LawDetailResponseDTO> result = new ArrayList<>();
        for (int i = 0; i < document.getSections().getCount(); i++) {
            Section documentSection = document.getSections().get(i);
            // 当前编、章、节、条数值
            Integer part = null, chapter = null, section = null, article;
            for (int j = 0; j < documentSection.getParagraphs().getCount(); j++) {
                Paragraph paragraph = documentSection.getParagraphs().get(j);
                String text = paragraph.getText();
                if (HzStringUtils.equals("Evaluation Warning: The document was created with Spire.Doc for JAVA.", text)) {
                    continue;
                }
                if (isBlank(text)) {
                    LawDetailResponseDTO lawDetailResponseDTO = new LawDetailResponseDTO();
                    lawDetailResponseDTO.setContent(text);
                    lawDetailResponseDTO.setContentType(EnumLawContentType.EMPTY_LINE);
                    result.add(lawDetailResponseDTO);
                    continue;
                }
                if (ReUtil.isMatch(CATALOGUE_REGEX, text)) {
                    LawDetailResponseDTO lawDetailResponseDTO = new LawDetailResponseDTO();
                    StringBuilder stringBuilder = new StringBuilder(text);
                    int tempIndex = j + 1;
                    String tempText = documentSection.getParagraphs().get(tempIndex).getText();
                    while (tempIndex < documentSection.getParagraphs().getCount() && !isBlank(tempText)) {
                        stringBuilder.append("\n");
                        stringBuilder.append(tempText);
                        tempIndex++;
                        if (tempIndex < documentSection.getParagraphs().getCount()) {
                            tempText = documentSection.getParagraphs().get(tempIndex).getText();
                        } else {
                            break;
                        }
                    }
                    j = tempIndex - 1;
                    lawDetailResponseDTO.setContent(stringBuilder.toString());
                    lawDetailResponseDTO.setContentType(EnumLawContentType.OTHER);
                    result.add(lawDetailResponseDTO);
                    continue;
                }
                LawDetailResponseDTO lawDetailResponseDTO = new LawDetailResponseDTO();
                if (ReUtil.isMatch(PART_PATTERN, text)) {
                    // 编
                    part = ChineseToNumberUtils.convertNumber(ReUtil.getGroup1(PART_PATTERN, text));
                    lawDetailResponseDTO.setContentType(EnumLawContentType.PART);
                    lawDetailResponseDTO.setPart(part);
                } else if (ReUtil.isMatch(CHAPTER_PATTERN, text)) {
                    // 章
                    chapter = ChineseToNumberUtils.convertNumber(ReUtil.getGroup1(CHAPTER_PATTERN, text));
                    lawDetailResponseDTO.setContentType(EnumLawContentType.CHAPTER);
                    lawDetailResponseDTO.setPart(part);
                    lawDetailResponseDTO.setChapter(chapter);
                } else if (ReUtil.isMatch(SECTION_PATTERN, text)) {
                    // 节
                    section = ChineseToNumberUtils.convertNumber(ReUtil.getGroup1(SECTION_PATTERN, text));
                    lawDetailResponseDTO.setContentType(EnumLawContentType.SECTION);
                    lawDetailResponseDTO.setPart(part);
                    lawDetailResponseDTO.setChapter(chapter);
                    lawDetailResponseDTO.setSection(section);
                } else if (ReUtil.isMatch(ARTICLE_PATTERN, text)) {
                    // 条
                    article = ChineseToNumberUtils.convertNumber(ReUtil.get(ARTICLE_PATTERN, text, 2));
                    boolean hasMore = text.endsWith("：") || (j + 1 < documentSection.getParagraphs().getCount() && noneMatch(documentSection.getParagraphs().get(j + 1).getText(), PART_PATTERN, CHAPTER_PATTERN, SECTION_PATTERN, ARTICLE_PATTERN));
                    if (hasMore) {
                        if (text.startsWith("　　")) {
                            text = text.substring(2);
                        }
                        // 说明还有具体的小条例或者更多信息
                        StringBuilder stringBuilder = new StringBuilder(text);
                        int tempIndex = j + 1;
                        String tempText = documentSection.getParagraphs().get(tempIndex).getText();
                        // 继续读取小条例或者更多信息
                        while (tempIndex < documentSection.getParagraphs().getCount() && noneMatch(tempText, PART_PATTERN, CHAPTER_PATTERN, SECTION_PATTERN, ARTICLE_PATTERN)) {
                            if (tempText.startsWith("　　")) {
                                tempText = tempText.substring(2);
                            }
                            stringBuilder.append("\n");
                            stringBuilder.append(tempText);
                            tempIndex++;
                            if (tempIndex < documentSection.getParagraphs().getCount()) {
                                tempText = documentSection.getParagraphs().get(tempIndex).getText();
                            } else {
                                break;
                            }
                        }
                        j = tempIndex - 1;
                        lawDetailResponseDTO.setContent(stringBuilder.toString());
                    }
                    lawDetailResponseDTO.setContentType(EnumLawContentType.ARTICLE);
                    lawDetailResponseDTO.setPart(part);
                    lawDetailResponseDTO.setChapter(chapter);
                    lawDetailResponseDTO.setSection(section);
                    lawDetailResponseDTO.setArticle(article);
                } else if (ReUtil.isMatch(FOREWORD_REGEX, text)) {
                    // 序言
                    lawDetailResponseDTO.setContentType(EnumLawContentType.FOREWORD);
                    StringBuilder stringBuilder = new StringBuilder(text).append("\n");
                    int tempIndex = j + 1;
                    String tempText = documentSection.getParagraphs().get(tempIndex).getText();
                    // 序言下一行为空 但是不确定是否只空一行 这里用while循环匹配到第一行非空序言
                    while (HzStringUtils.isBlank(tempText)) {
                        tempIndex++;
                        tempText = documentSection.getParagraphs().get(tempIndex).getText();
                    }
                    // 到了这里肯定是非空序言开头
                    while (true) {
                        if (tempText.startsWith("　　")) {
                            tempText = tempText.substring(2);
                        }
                        stringBuilder.append(tempText);
                        tempIndex++;
                        tempText = documentSection.getParagraphs().get(tempIndex).getText();
                        if (HzStringUtils.isBlank(tempText)) {
                            // 最后一行序言结束 下一行为空行 说明序言结束
                            break;
                        }
                        stringBuilder.append("\n");
                    }
                    j = tempIndex - 1;
                    lawDetailResponseDTO.setContent(stringBuilder.toString());
                } else {
                    if (text.contains("\u000B")) {
                        text = text.replace("\u000B", "");
                    }
                    lawDetailResponseDTO.setContent(text);
                    lawDetailResponseDTO.setContentType(EnumLawContentType.OTHER);
                    result.add(lawDetailResponseDTO);
                    continue;
                }
                if (lawDetailResponseDTO.getContent() == null) {
                    if (text.startsWith("　　")) {
                        text = text.substring(2);
                    }
                    lawDetailResponseDTO.setContent(text);
                }
                result.add(lawDetailResponseDTO);
            }
        }
        return result;
    }

    private static List<LawDetailResponseDTO> readFromDocxWithoutPattern(Document document) {
        List<LawDetailResponseDTO> result = new ArrayList<>();
        for (int i = 0; i < document.getSections().getCount(); i++) {
            Section documentSection = document.getSections().get(i);
            // 当前编、章、节、条数值
            for (int j = 0; j < documentSection.getParagraphs().getCount(); j++) {
                Paragraph paragraph = documentSection.getParagraphs().get(j);
                String text = paragraph.getText();
                if (HzStringUtils.equals("Evaluation Warning: The document was created with Spire.Doc for JAVA.", text)) {
                    continue;
                }
                LawDetailResponseDTO responseDTO = new LawDetailResponseDTO();
                responseDTO.setContentType(EnumLawContentType.OTHER);
                responseDTO.setContent(text);
                result.add(responseDTO);
            }
        }
        return result;
    }

    /**
     * 从word文件中读取内容
     *
     * @param path word文件路径
     */
    public static List<LawDetailResponseDTO> readFromDocx(String path, FileFormat fileFormat) {
        Document document = new Document(getFileStream(path), fileFormat);
        if (document.findPattern(ARTICLE_PATTERN) != null) {
            return readFromDocxWithArticlePattern(document);
        }
        return readFromDocxWithoutPattern(document);
    }

//    public static void main(String[] args) {
//        Document document = new Document();
//        document.loadFromFile("C:\\Users\\PC_Admin\\Downloads\\40d45cf469254fc6b15b0229efcfbff9.docx");
//        String text = document.getText();
//    }

//    /**
//     * 从pdf文件中读取内容
//     *
//     * @param path pdf文件路径
//     */
//    public static List<String> readFromPdf(String path) {
//        PdfDocument pdfDocument = new PdfDocument(getFileStream(path));
//        PdfPageBase page;
//        List<String> result = new ArrayList<>();
//        //遍历PDF页面，获取每个页面的文本并添加到StringBuilder对象
//        for (int i = 0; i < pdfDocument.getPages().getCount(); i++) {
//            page = pdfDocument.getPages().get(i);
//            result.add(page.extractText(true, true, true));
//        }
//        Document document = new Document();
//
//        return result;
//    }

//    public static void main(String[] args) {
////        String pdfPath = "https://wb.flk.npc.gov.cn/dfxfg/PDF/5358b7b1002746a897ec7818a9293977.pdf";
//        PdfDocument pdfDocument = new PdfDocument("C:\\Users\\PC_Admin\\Downloads\\5358b7b1002746a897ec7818a9293977.pdf");
//        pdfDocument.getConvertOptions().setConvertToWordUsingFlow(true);
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        pdfDocument.saveToStream(outputStream, com.spire.pdf.FileFormat.DOCX);
//        Document document = new Document(new ByteArrayInputStream(outputStream.toByteArray()));
//        String text = document.getText();
//        System.out.println(text);
//    }

    /**
     * 判断文本是否满足任一正则
     *
     * @param text     待匹配文本
     * @param patterns 待匹配正则
     * @return 都不满足返回true
     */
    private static boolean noneMatch(String text, Pattern... patterns) {
        for (Pattern pattern : patterns) {
            if (ReUtil.isMatch(pattern, text)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取文件的二进制流
     *
     * @param path 文件路径
     */
    private static InputStream getFileStream(String path) {
        return new ByteArrayInputStream(getFileBytes(path));
    }

    /**
     * 获取文件的字节数组
     *
     * @param path 文件路径
     */
    private static byte[] getFileBytes(String path) {
        HttpRequest httpRequest = HttpRequest.get(FILE_URL + path).setFollowRedirects(true);
        @Cleanup
        HttpResponse response = safeExecute(httpRequest);
        return response.bodyBytes();
    }

    /**
     * 安全执行http请求 网站有安全机制校验cookie等信息
     */
    private static HttpResponse safeExecute(HttpRequest httpRequest) {
        if (HzStringUtils.isNotBlank(getCookieStr())) {
            httpRequest.cookie(getCookieStr());
        }
//        ThreadUtil.safeSleep(500);
        HttpResponse response = httpRequest.setConnectionTimeout(10000).setReadTimeout(10000)
//                .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
//                .header(Header.CONNECTION, "close")
                .execute();
        String firstResponseCookieStr = response.getCookieStr();
        if (HzStringUtils.isNotBlank(firstResponseCookieStr)) {
            setCookieStr(firstResponseCookieStr);
        }
        int status = response.getStatus();
        boolean isOk = (status >= HttpStatus.HTTP_MULT_CHOICE && status < HttpStatus.HTTP_BAD_REQUEST)
                || !JSONUtil.isTypeJSON(response.body());
        if (isOk) {
            String responseCookieStr = response.getCookieStr();
            if (HzStringUtils.isNotBlank(responseCookieStr)) {
                setCookieStr(responseCookieStr);
                httpRequest.cookie(getCookieStr());
                response = httpRequest.execute();
                if (response.isOk()) {
                    return response;
                }
            }
        }
        return response;
    }

    public static void temp() {
        List<File> files = FileUtil.loopFiles("C:\\Users\\PC_Admin\\Desktop\\杭州工作制度");
        for (File file : files) {
            LawDO lawDO = new LawDO();
            lawDO.setOuterId("random_" + RandomUtil.randomString(20));
            lawDO.setTitle(file.getName().replace(".docx", ""));
            lawDO.setType(EnumLawType.LOCAL_REGULATIONS);
            lawDO.setStatus(EnumLawStatus.EFFECTIVE);
            lawDO.setSubject("");
            lawDO.setDataSource(EnumLawSource.MEASURES_FOR_THE_ESTABLISHMENT_OF_LOCAL_REGULATIONS_IN_HANGZHOU);
//            lawDO.setDocFileUrl();
//            lawDO.setPdfFileUrl();
            HzSpringUtils.getBean(ILawService.class).save(lawDO);
            Document document = new Document(file.getAbsolutePath(), FileFormat.Docx);
            List<LawDetailResponseDTO> list;
            if (file.getName().startsWith("杭州市人大常委会建立立法基层联系点办法")) {
                list = readFromDocxWithoutPattern(document);
            } else {
                list = readFromDocxWithArticlePattern(document);
            }
            List<LawContentDO> lawContentList = list.stream().map(responseDTO -> {
                LawContentDO contentDO = new LawContentDO();
                contentDO.setOuterId(lawDO.getOuterId());
                contentDO.setType(lawDO.getType());
                contentDO.setStatus(lawDO.getStatus());
                contentDO.setTitle(lawDO.getTitle());
                contentDO.setSubject(lawDO.getSubject());
                contentDO.setPart(responseDTO.getPart());
                contentDO.setChapter(responseDTO.getChapter());
                contentDO.setSection(responseDTO.getSection());
                contentDO.setArticle(responseDTO.getArticle());
                contentDO.setContentType(responseDTO.getContentType());
                contentDO.setContent(responseDTO.getContent());
                contentDO.setDataSource(lawDO.getDataSource());
                return contentDO;
            }).collect(Collectors.toList());
            HzSpringUtils.getBean(ILawContentService.class).insertBatch(lawContentList);
        }
    }
}
