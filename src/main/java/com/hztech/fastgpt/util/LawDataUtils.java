package com.hztech.fastgpt.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.mutable.MutableObj;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.*;
import cn.hutool.http.*;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hztech.fastgpt.dao.po.ArticleSmartDO;
import com.hztech.fastgpt.dao.po.LawContentDO;
import com.hztech.fastgpt.dao.po.LawDO;
import com.hztech.fastgpt.dao.wrapper.LawQuery;
import com.hztech.fastgpt.mapper.wrapper.ArticleSmartMapperWrapper;
import com.hztech.fastgpt.mapper.wrapper.LawContentMapperWrapper;
import com.hztech.fastgpt.model.dto.response.CountryLawBasicDataResponseDTO;
import com.hztech.fastgpt.model.dto.response.CountryLawDetailDataResponseDTO;
import com.hztech.fastgpt.model.dto.response.LawDetailResponseDTO;
import com.hztech.fastgpt.model.dto.response.LawResponseDTO;
import com.hztech.fastgpt.model.enums.EnumLawContentType;
import com.hztech.fastgpt.model.enums.EnumLawSource;
import com.hztech.fastgpt.model.enums.EnumLawStatus;
import com.hztech.fastgpt.model.enums.EnumLawType;
import com.hztech.fastgpt.service.ILawService;
import com.hztech.util.*;
import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import com.spire.doc.Section;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.documents.TextSelection;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.slf4j.MDC;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
//                    HzSpringUtils.getBean(ILawContentService.class).insertBatch(list);
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
                String text = paragraph.getListText() + paragraph.getText();
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
                    String tempText = documentSection.getParagraphs().get(tempIndex).getListText() + documentSection.getParagraphs().get(tempIndex).getText();
                    while (isBlank(tempText)) {
                        tempIndex++;
                        tempText = documentSection.getParagraphs().get(tempIndex).getListText() + documentSection.getParagraphs().get(tempIndex).getText();
                    }
                    Pattern firstLinePattern = null;
                    Integer firstLineIndex = null;
                    while (tempIndex < documentSection.getParagraphs().getCount() && !isBlank(tempText)) {
                        if (firstLinePattern == null && firstLineIndex == null) {
                            if (ReUtil.isMatch(PART_PATTERN, tempText)) {
                                // 编
                                firstLinePattern = PART_PATTERN;
                                firstLineIndex = ChineseToNumberUtils.convertNumber(ReUtil.getGroup1(PART_PATTERN, tempText));
                            } else if (ReUtil.isMatch(CHAPTER_PATTERN, tempText)) {
                                // 章
                                firstLinePattern = CHAPTER_PATTERN;
                                firstLineIndex = ChineseToNumberUtils.convertNumber(ReUtil.getGroup1(CHAPTER_PATTERN, tempText));
                            } else if (ReUtil.isMatch(SECTION_PATTERN, tempText)) {
                                // 节
                                firstLinePattern = SECTION_PATTERN;
                                firstLineIndex = ChineseToNumberUtils.convertNumber(ReUtil.getGroup1(SECTION_PATTERN, tempText));
                            } else if (ReUtil.isMatch(ARTICLE_PATTERN, tempText)) {
                                // 条
                                firstLinePattern = ARTICLE_PATTERN;
                                firstLineIndex = ChineseToNumberUtils.convertNumber(ReUtil.get(ARTICLE_PATTERN, tempText, 2));
                            }
                        } else {
                            if (ReUtil.isMatch(PART_PATTERN, tempText) && ObjectUtil.equals(firstLineIndex, ChineseToNumberUtils.convertNumber(ReUtil.getGroup1(PART_PATTERN, tempText)))) {
                                break;
                            } else if (ReUtil.isMatch(CHAPTER_PATTERN, tempText) && ObjectUtil.equals(firstLineIndex, ChineseToNumberUtils.convertNumber(ReUtil.getGroup1(CHAPTER_PATTERN, tempText)))) {
                                // 章
                                break;
                            } else if (ReUtil.isMatch(SECTION_PATTERN, tempText) && ObjectUtil.equals(firstLineIndex, ChineseToNumberUtils.convertNumber(ReUtil.getGroup1(SECTION_PATTERN, tempText)))) {
                                // 节
                                break;
                            } else if (ReUtil.isMatch(ARTICLE_PATTERN, tempText) && ObjectUtil.equals(firstLineIndex, ChineseToNumberUtils.convertNumber(ReUtil.get(ARTICLE_PATTERN, tempText, 2)))) {
                                // 条
                                break;
                            }
                        }
                        stringBuilder.append("\n");
                        stringBuilder.append(tempText);
                        tempIndex++;
                        if (tempIndex < documentSection.getParagraphs().getCount()) {
                            tempText = documentSection.getParagraphs().get(tempIndex).getListText() + documentSection.getParagraphs().get(tempIndex).getText();
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
                    boolean hasMore = text.endsWith("：") || (j + 1 < documentSection.getParagraphs().getCount() && noneMatch(documentSection.getParagraphs().get(j + 1).getListText() + documentSection.getParagraphs().get(j + 1).getText(), PART_PATTERN, CHAPTER_PATTERN, SECTION_PATTERN, ARTICLE_PATTERN));
                    if (hasMore) {
                        if (text.startsWith("　　")) {
                            text = text.substring(2);
                        }
                        // 说明还有具体的小条例或者更多信息
                        StringBuilder stringBuilder = new StringBuilder(text);
                        int tempIndex = j + 1;
                        String tempText = documentSection.getParagraphs().get(tempIndex).getListText() + documentSection.getParagraphs().get(tempIndex).getText();
                        // 继续读取小条例或者更多信息
                        while (tempIndex < documentSection.getParagraphs().getCount() && noneMatch(tempText, PART_PATTERN, CHAPTER_PATTERN, SECTION_PATTERN, ARTICLE_PATTERN)) {
                            if (tempText.startsWith("　　")) {
                                tempText = tempText.substring(2);
                            }
                            stringBuilder.append("\n");
                            stringBuilder.append(tempText);
                            tempIndex++;
                            if (tempIndex < documentSection.getParagraphs().getCount()) {
                                tempText = documentSection.getParagraphs().get(tempIndex).getListText() + documentSection.getParagraphs().get(tempIndex).getText();
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
                    String tempText = documentSection.getParagraphs().get(tempIndex).getListText() + documentSection.getParagraphs().get(tempIndex).getText();
                    // 序言下一行为空 但是不确定是否只空一行 这里用while循环匹配到第一行非空序言
                    while (HzStringUtils.isBlank(tempText)) {
                        tempIndex++;
                        tempText = documentSection.getParagraphs().get(tempIndex).getListText() + documentSection.getParagraphs().get(tempIndex).getText();
                    }
                    // 到了这里肯定是非空序言开头
                    while (true) {
                        if (tempText.startsWith("　　")) {
                            tempText = tempText.substring(2);
                        }
                        stringBuilder.append(tempText);
                        tempIndex++;
                        tempText = documentSection.getParagraphs().get(tempIndex).getListText() + documentSection.getParagraphs().get(tempIndex).getText();
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
                String text = paragraph.getListText() + paragraph.getText();
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
        return readFromDocx(document);
    }

    /**
     * 从word文件中读取内容
     */
    public static List<LawDetailResponseDTO> readFromDocx(Document document) {
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

//    public static void main(String[] args) {
//        temp2();
////        PdfDocument pdfDocument = new PdfDocument("C:\\Users\\PC_Admin\\Documents\\2024_10_08_目录\\一、议事规则.pdf");
////        pdfDocument.saveToFile("C:\\Users\\PC_Admin\\Documents\\2024_10_08_目录\\一、议事规则2.docx", com.spire.pdf.FileFormat.DOCX);
//    }
//
//    public static void temp1() {
//        List<File> files = FileUtil.loopFiles("C:\\Users\\PC_Admin\\Documents\\2024_10_08_四、监督工作(1)\\with_pattern");
//        for (File file : files) {
////            if (file.getName().startsWith("~") || !file.getName().startsWith("杭州市人大新闻宣传工作规定")) {
////                continue;
////            }
//            LawDO lawDO = new LawDO();
//            lawDO.setOuterId("random_" + RandomUtil.randomString(20));
//            lawDO.setTitle(file.getName().replace(".docx", ""));
//            lawDO.setType(EnumLawType.LOCAL_REGULATIONS);
//            lawDO.setStatus(EnumLawStatus.EFFECTIVE);
//            lawDO.setSubject("");
//            lawDO.setDataSource(EnumLawSource.MEASURES_FOR_THE_ESTABLISHMENT_OF_LOCAL_REGULATIONS_IN_HANGZHOU);
////            lawDO.setDocFileUrl();
////            lawDO.setPdfFileUrl();
//            HzSpringUtils.getBean(ILawService.class).save(lawDO);
//            Document document = new Document(file.getAbsolutePath(), FileFormat.Docx);
//            List<LawDetailResponseDTO> list = readFromDocxWithArticlePatternV2(document);
//            List<LawContentDO> lawContentList = list.stream().map(responseDTO -> {
//                LawContentDO contentDO = new LawContentDO();
//                contentDO.setOuterId(lawDO.getOuterId());
//                contentDO.setType(lawDO.getType());
//                contentDO.setStatus(lawDO.getStatus());
//                contentDO.setTitle(lawDO.getTitle());
//                contentDO.setSubject(lawDO.getSubject());
//                contentDO.setPart(responseDTO.getPart());
//                contentDO.setChapter(responseDTO.getChapter());
//                contentDO.setSection(responseDTO.getSection());
//                contentDO.setArticle(responseDTO.getArticle());
//                contentDO.setContentType(responseDTO.getContentType());
//                contentDO.setContent(responseDTO.getContent());
//                contentDO.setDataSource(lawDO.getDataSource());
//                return contentDO;
//            }).collect(Collectors.toList());
//            HzSpringUtils.getBean(ILawContentService.class).insertBatch(lawContentList);
//        }
//    }
//
//    public static void temp2() {
//        List<File> files = FileUtil.loopFiles("C:\\Users\\PC_Admin\\Documents\\2024_10_08_四、监督工作(1)\\without_pattern");
//        for (File file : files) {
////            if (file.getName().startsWith("~") || !file.getName().startsWith("杭州市人大信息工作计分办法")) {
////                continue;
////            }
//            LawDO lawDO = new LawDO();
//            lawDO.setOuterId("random_" + RandomUtil.randomString(20));
//            lawDO.setTitle(file.getName().replace(".docx", ""));
//            lawDO.setType(EnumLawType.LOCAL_REGULATIONS);
//            lawDO.setStatus(EnumLawStatus.EFFECTIVE);
//            lawDO.setSubject("");
//            lawDO.setDataSource(EnumLawSource.MEASURES_FOR_THE_ESTABLISHMENT_OF_LOCAL_REGULATIONS_IN_HANGZHOU);
////            lawDO.setDocFileUrl();
////            lawDO.setPdfFileUrl();
//            HzSpringUtils.getBean(ILawService.class).save(lawDO);
//            Document document = new Document(file.getAbsolutePath(), FileFormat.Docx);
//            List<LawDetailResponseDTO> list = readFromDocxWithoutPatternV2(document);
//            List<LawContentDO> lawContentList = list.stream().map(responseDTO -> {
//                LawContentDO contentDO = new LawContentDO();
//                contentDO.setOuterId(lawDO.getOuterId());
//                contentDO.setType(lawDO.getType());
//                contentDO.setStatus(lawDO.getStatus());
//                contentDO.setTitle(lawDO.getTitle());
//                contentDO.setSubject(lawDO.getSubject());
//                contentDO.setPart(responseDTO.getPart());
//                contentDO.setChapter(responseDTO.getChapter());
//                contentDO.setSection(responseDTO.getSection());
//                contentDO.setArticle(responseDTO.getArticle());
//                contentDO.setContentType(responseDTO.getContentType());
//                contentDO.setContent(responseDTO.getContent());
//                contentDO.setDataSource(lawDO.getDataSource());
//                return contentDO;
//            }).collect(Collectors.toList());
//            HzSpringUtils.getBean(ILawContentService.class).insertBatch(lawContentList);
//        }
//    }
//

    /**
     * 读取正常格式的docx
     *
     * @param document docx文档
     */
    @SuppressWarnings("all")
    private static List<LawDetailResponseDTO> readFromDocxWithArticlePatternV2(Document document) {
        List<LawDetailResponseDTO> result = new ArrayList<>();
        String allText = document.getText();
        List<String> list = StrSplitter.split(allText, "\n", true, false);
        Integer part = null, chapter = null, section = null, article;
        for (int i = 0; i < list.size(); i++) {
            // 当前编、章、节、条数值
            String text = list.get(i);
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
                int tempIndex = i + 1;
                String tempText = list.get(tempIndex);
                while (isBlank(tempText)) {
                    tempIndex++;
                    tempText = list.get(tempIndex);
                }
                while (tempIndex < list.size() && !isBlank(tempText)) {
                    stringBuilder.append("\n");
                    stringBuilder.append(tempText);
                    tempIndex++;
                    if (tempIndex < list.size()) {
                        tempText = list.get(tempIndex);
                    } else {
                        break;
                    }
                }
                i = tempIndex - 1;
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
                boolean hasMore = text.endsWith("：") || (i + 1 < list.size() && noneMatch(list.get(i + 1), PART_PATTERN, CHAPTER_PATTERN, SECTION_PATTERN, ARTICLE_PATTERN));
                if (hasMore) {
                    if (text.startsWith("　　")) {
                        text = text.substring(2);
                    }
                    // 说明还有具体的小条例或者更多信息
                    StringBuilder stringBuilder = new StringBuilder(text);
                    int tempIndex = i + 1;
                    String tempText = list.get(tempIndex);
                    // 继续读取小条例或者更多信息
                    while (tempIndex < list.size() && noneMatch(tempText, PART_PATTERN, CHAPTER_PATTERN, SECTION_PATTERN, ARTICLE_PATTERN)) {
                        if (tempText.startsWith("　　")) {
                            tempText = tempText.substring(2);
                        }
                        stringBuilder.append("\n");
                        stringBuilder.append(tempText);
                        tempIndex++;
                        if (tempIndex < list.size()) {
                            tempText = list.get(tempIndex);
                        } else {
                            break;
                        }
                    }
                    i = tempIndex - 1;
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
                int tempIndex = i + 1;
                String tempText = list.get(tempIndex);
                // 序言下一行为空 但是不确定是否只空一行 这里用while循环匹配到第一行非空序言
                while (HzStringUtils.isBlank(tempText)) {
                    tempIndex++;
                    tempText = list.get(tempIndex);
                }
                // 到了这里肯定是非空序言开头
                while (true) {
                    if (tempText.startsWith("　　")) {
                        tempText = tempText.substring(2);
                    }
                    stringBuilder.append(tempText);
                    tempIndex++;
                    tempText = list.get(tempIndex);
                    if (HzStringUtils.isBlank(tempText)) {
                        // 最后一行序言结束 下一行为空行 说明序言结束
                        break;
                    }
                    stringBuilder.append("\n");
                }
                i = tempIndex - 1;
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
        return result;
    }

    private static List<LawDetailResponseDTO> readFromDocxWithoutPatternV2(Document document) {
        List<LawDetailResponseDTO> result = new ArrayList<>();
        String allText = document.getText();
        List<String> list = StrSplitter.split(allText, "\n", true, false);
        for (String text : list) {
            if (HzStringUtils.equals("Evaluation Warning: The document was created with Spire.Doc for JAVA.", text)) {
                continue;
            }
            LawDetailResponseDTO responseDTO = new LawDetailResponseDTO();
            responseDTO.setContentType(EnumLawContentType.OTHER);
            responseDTO.setContent(text);
            result.add(responseDTO);
        }
        return result;
    }

    private static final Pattern EFFECTIVE_PATTERN = Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日).{0,4}(起施行|起实施)");

    private static final Pattern PUBLISH_PATTERN = Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日).*(发布|公布)");

//    @SneakyThrows
//    public static void main(String[] args) {
//        org.jsoup.nodes.Document doc = Jsoup.parse(URLUtil.url("https://www.hangzhou.gov.cn/art/2003/10/10/art_1229063379_1720978.html"), 5000);
//        String html = doc.getElementsByClass("article").get(0).html();
//        Document document = new Document(new ByteArrayInputStream(html.getBytes()), FileFormat.Html);
//        TextSelection textSelection = document.findPattern(EFFECTIVE_PATTERN);
//        if (textSelection != null && textSelection.getCount() > 0) {
//            String text = textSelection.getAsOneRange().getText();
//            System.out.println(text);
//        }
//    }

    @SneakyThrows
    public static void articleSmart() {
        ArticleSmartMapperWrapper mapper = HzSpringUtils.getBean(ArticleSmartMapperWrapper.class);
        List<ArticleSmartDO> all = mapper.findAll();
        for (ArticleSmartDO articleSmartDO : all) {
            if ("杭州市人民政府门户网站-地方性法规".equals(articleSmartDO.getSource()) && LawQuery.query().where.title().eq(articleSmartDO.getTitle()).end().to().count() > 0) {
                System.out.println(articleSmartDO.getTitle() + "：已存在跳过");
                continue;
            }
            LawDO lawDO = new LawDO();
            lawDO.setOuterId("random_" + RandomUtil.randomString(20));
            lawDO.setTitle(articleSmartDO.getTitle().replace("\u200b", ""));
            lawDO.setOriginalUrl(articleSmartDO.getOriginal());
            org.jsoup.nodes.Document doc = Jsoup.parse(URLUtil.url(articleSmartDO.getOriginal()), 5000);
            Document document;
            if ("杭州市人民政府门户网站-地方性法规".equals(articleSmartDO.getSource())) {
                String publish = doc.getElementsByClass("xxgkinfo").get(0).getAllElements().get(11).text();
                lawDO.setPublish(DateUtil.parse(publish).toString(DatePattern.NORM_DATETIME_PATTERN));
                String html = doc.getElementsByClass("article").get(0).html();
                document = new Document(new ByteArrayInputStream(html.getBytes()), FileFormat.Html);
                TextSelection effectiveTextSelection = document.findPattern(EFFECTIVE_PATTERN);
                if (effectiveTextSelection != null && effectiveTextSelection.getCount() > 0) {
                    String text = effectiveTextSelection.getAsOneRange().getText();
                    String effective = ReUtil.getGroup1(EFFECTIVE_PATTERN, text);
                    lawDO.setEffective(DateUtil.parse(effective).toString(DatePattern.NORM_DATETIME_PATTERN));
                } else {
                    lawDO.setEffective(lawDO.getPublish());
                }
                lawDO.setType(EnumLawType.LOCAL_REGULATIONS);
                lawDO.setDataSource(EnumLawSource.HANGZHOU_MUNICIPAL_PEOPLE_GOVERNMENT_PORTAL_WEBSITE_LOCAL_REGULATIONS);
                lawDO.setSubject("杭州市人民代表大会常务委员会");
                // 时效性
                String status = doc.getElementsByClass("xxgkinfo").get(0).getAllElements().get(14).text();
                EnumLawStatus lawStatus = HzEnumUtils.fromDesc(EnumLawStatus.class, status);
                if (lawStatus == null) {
                    if ("失效".equals(status) || "废止".equals(status)) {
                        lawStatus = EnumLawStatus.REPEALED;
                    } else {
                        throw new IllegalArgumentException("未知时效性：" + status);
                    }
                }
                lawDO.setStatus(lawStatus);
            } else {
                String html = doc.getElementsByClass("zc_article_con").get(0).html();
                document = new Document(new ByteArrayInputStream(html.getBytes()), FileFormat.Html);
                TextSelection effectiveTextSelection = document.findPattern(EFFECTIVE_PATTERN);
                if (effectiveTextSelection != null && effectiveTextSelection.getCount() > 0) {
                    String text = effectiveTextSelection.getAsOneRange().getText();
                    String effective = ReUtil.getGroup1(EFFECTIVE_PATTERN, text);
                    lawDO.setEffective(DateUtil.parse(effective).toString(DatePattern.NORM_DATETIME_PATTERN));
                }
                lawDO.setType(EnumLawType.NONE);
                lawDO.setDataSource(EnumLawSource.HANGZHOU_MUNICIPAL_PEOPLE_GOVERNMENT_PORTAL_GOVERNMENT_REGULATIONS_DATABASE);
                lawDO.setSubject("杭州市人民政府");
                TextSelection publishTextSelection = document.findPattern(PUBLISH_PATTERN);
                if (publishTextSelection != null && publishTextSelection.getCount() > 0) {
                    String text = publishTextSelection.getAsOneRange().getText();
                    String publish = ReUtil.getGroup1(PUBLISH_PATTERN, text);
                    lawDO.setPublish(DateUtil.parse(publish).toString(DatePattern.NORM_DATETIME_PATTERN));
                }
                lawDO.setStatus(EnumLawStatus.EFFECTIVE);
                String[] fileUrls = articleSmartDO.getFileList().split(",");
                String docxFileUrl = "/fstore/" + HzFileUtils.getName(HttpUtil.decodeParamMap(fileUrls[0], StandardCharsets.UTF_8).get("filename"));
                String pdfFileUrl = "/fstore/" + HzFileUtils.getName(HttpUtil.decodeParamMap(fileUrls[1], StandardCharsets.UTF_8).get("filename"));
                FileUtil.writeBytes(HttpUtil.downloadBytes(fileUrls[0]), docxFileUrl);
                FileUtil.writeBytes(HttpUtil.downloadBytes(fileUrls[1]), pdfFileUrl);
                lawDO.setDocFileUrl(docxFileUrl);
                lawDO.setPdfFileUrl(pdfFileUrl);
            }

            HzSpringUtils.getBean(ILawService.class).save(lawDO);
            List<LawDetailResponseDTO> list;
            if (document.findPattern(ARTICLE_PATTERN) != null) {
                list = readFromDocxWithArticlePatternV2(document);
            } else {
                list = readFromDocxWithoutPatternV2(document);
            }
            List<LawContentDO> lawContentList = list.stream().map(responseDTO -> {
                LawContentDO contentDO = new LawContentDO();
                contentDO.setOuterId(lawDO.getOuterId());
                contentDO.setType(lawDO.getType());
                contentDO.setStatus(lawDO.getStatus());
                contentDO.setTitle(lawDO.getTitle());
                contentDO.setSubject(lawDO.getSubject());
                contentDO.setEffective(lawDO.getEffective());
                contentDO.setPublish(lawDO.getPublish());
                contentDO.setPart(responseDTO.getPart());
                contentDO.setChapter(responseDTO.getChapter());
                contentDO.setSection(responseDTO.getSection());
                contentDO.setArticle(responseDTO.getArticle());
                contentDO.setContentType(responseDTO.getContentType());
                contentDO.setContent(responseDTO.getContent());
                contentDO.setDataSource(lawDO.getDataSource());
                contentDO.setDocFileUrl(lawDO.getDocFileUrl());
                contentDO.setPdfFileUrl(lawDO.getPdfFileUrl());
                return contentDO;
            }).collect(Collectors.toList());
            HzSpringUtils.getBean(LawContentMapperWrapper.class).insertBatch(lawContentList);
        }
    }

//    @SneakyThrows
//    public static void main(String[] args) {
////        String string = FileUtil.readUtf8String(FileUtil.touch("D:\\fstore\\aaa.ofd"));
////        System.out.println(string);
////        Document document = new Document(Files.newInputStream(FileUtil.touch("D:\\fstore\\aaa.ofd").toPath()), FileFormat.Auto);
////        document.saveToFile("D:\\fstore\\bbb.docx", FileFormat.Docx);
//
////        XWPFDocument docx = new XWPFDocument(FileUtil.getInputStream("C:\\Users\\PC_Admin\\Downloads\\c7e15807753448a58309da8ec127cebc.docx"));
////        for (XWPFHeader header : docx.getHeaderList()) {
////            header.clearHeaderFooter();
////        }
////        for (XWPFFooter footer : docx.getFooterList()) {
////            footer.clearHeaderFooter();
////        }
////        for (XWPFParagraph paragraph : docx.getParagraphs()) {
////
////        }
////        XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
////        String text = extractor.getText();
////        System.out.println(text);
//
////        System.out.println(HzIdUtils.nextSnowflakeId());
////        System.out.println(HzIdUtils.nextSnowflakeIdYit());
////        Document document = new Document("C:\\Users\\PC_Admin\\Downloads\\80e776e3302a4b9a89d06423bd9059fd.docx" , FileFormat.Docx);
////        List<LawDetailResponseDTO> list = readFromDocxWithArticlePattern(document);
////        String now = DateUtil.now();
////        for (LawDetailResponseDTO responseDTO : list) {
////            StringBuilder stringBuilder = new StringBuilder("INSERT INTO law_content VALUES(");
////            stringBuilder.append(HzIdUtils.nextSnowflakeIdYit())
////                    .append(",")
////                    .append("'ZmY4MDgxODE5MDE0ZWMzZjAxOTA1ZGE0OTY4ZDUyYmY%3D',")
////                    .append(5).append(",").append(1).append(",")
////                    .append("'杭州老字号传承与发展条例',")
////                    .append("'杭州市人民代表大会常务委员会',").append("'2024-07-01 00:00:00',").append("'2024-06-04 00:00:00',")
////                    .append(ObjectUtil.toString(responseDTO.getPart())).append(",").append(ObjectUtil.toString(responseDTO.getChapter())).append(",")
////                    .append(ObjectUtil.toString(responseDTO.getSection())).append(",").append(ObjectUtil.toString(responseDTO.getArticle())).append(",")
////                    .append(responseDTO.getContentType().getValue()).append(",'").append(responseDTO.getContent()).append("',")
////                    .append(0).append(",").append("'/fstore/80e776e3302a4b9a89d06423bd9059fd.docx',").append("null,'").append(now).append("',").append("'").append(now).append("');");
////            System.out.println(stringBuilder);
////        }
////        System.out.println(JSONUtil.toJsonStr(list));
//
//        String json = FileUtil.readUtf8String("D:\\project\\fastgpt\\src\\main\\java\\com\\hztech\\fastgpt\\a.json");
//        List<String> ids = JSONUtil.parseArray(json).stream().map(item -> JSONUtil.parseObj(item).getStr("id")).collect(Collectors.toList());
//        String join = "(\"" + CollUtil.join(ids, "\",\"") + "\")";
//        System.out.println(join);
//    }

//    public static void rebuildLawContent() {
//        LawMapperWrapper lawMapperWrapper = HzSpringUtils.getBean(LawMapperWrapper.class);
//        List<LawDO> all = lawMapperWrapper.findAll();
//        LawContentBakMapperWrapper lawContentBakMapperWrapper = HzSpringUtils.getBean(LawContentBakMapperWrapper.class);
//        LawContentBakQuery lawContentBakQuery = lawContentBakMapperWrapper.query().select.outerId().end().groupBy.outerId().end();
//        List<String> outerIds = lawContentBakMapperWrapper.findFieldListByQuery(lawContentBakQuery);
//        for (LawDO law : all) {
//            if (outerIds.contains(law.getOuterId())) {
//                continue;
//            }
//            if (law.getDataSource() == EnumLawSource.NATIONAL_LAWS_AND_REGULATIONS_DATABASE) {
//                // 重新构建内容
//                String docFileUrl = law.getDocFileUrl();
//                FileFormat fileFormat = docFileUrl.endsWith(".docx") ? FileFormat.Docx : docFileUrl.endsWith(".doc") ? FileFormat.Auto : FileFormat.Html;
//                log.info("url:{}", docFileUrl);
//                ByteArrayInputStream inputStream = new ByteArrayInputStream(HttpUtil.downloadBytes("http://192.168.1.13:8080" + docFileUrl));
//                Document document = new Document(inputStream, fileFormat);
//                List<LawDetailResponseDTO> list = readFromDocx(document);
//                List<LawContentBakDO> lawContentDOList = list.stream().map(responseDTO -> {
//                    LawContentBakDO contentDO = new LawContentBakDO();
//                    contentDO.setOuterId(law.getOuterId());
//                    contentDO.setType(law.getType());
//                    contentDO.setStatus(law.getStatus());
//                    contentDO.setTitle(law.getTitle());
//                    contentDO.setSubject(law.getSubject());
//                    contentDO.setEffective(law.getEffective());
//                    contentDO.setPublish(law.getPublish());
//                    contentDO.setPart(responseDTO.getPart());
//                    contentDO.setChapter(responseDTO.getChapter());
//                    contentDO.setSection(responseDTO.getSection());
//                    contentDO.setArticle(responseDTO.getArticle());
//                    contentDO.setContentType(responseDTO.getContentType());
//                    contentDO.setContent(responseDTO.getContent());
//                    contentDO.setDataSource(law.getDataSource());
//                    contentDO.setDocFileUrl(law.getDocFileUrl());
//                    contentDO.setPdfFileUrl(law.getPdfFileUrl());
//                    return contentDO;
//                }).collect(Collectors.toList());
//                lawContentBakMapperWrapper.insertBatch(lawContentDOList);
//            } else {
//                LawContentMapperWrapper lawContentMapperWrapper = HzSpringUtils.getBean(LawContentMapperWrapper.class);
//                LawContentQuery lawContentQuery = lawContentMapperWrapper.query().where.outerId().eq(law.getOuterId()).end();
//                List<LawContentDO> list = lawContentMapperWrapper.findListByQuery(lawContentQuery);
//                List<LawContentBakDO> lawContentBakDOS = BeanUtil.copyToList(list, LawContentBakDO.class, CopyOptions.create().setIgnoreProperties("id"));
//                lawContentBakMapperWrapper.insertBatch(lawContentBakDOS);
//            }
//        }
//    }

}
