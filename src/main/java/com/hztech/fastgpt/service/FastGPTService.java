package com.hztech.fastgpt.service;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hztech.exception.HzRuntimeException;
import com.hztech.fastgpt.ServiceRequests;
import com.hztech.fastgpt.entity.ChatMessage;
import com.hztech.fastgpt.model.dto.request.ChatCompletionsRequestDTO;
import com.hztech.fastgpt.model.dto.request.PushDataRequestDTO;
import com.hztech.fastgpt.model.dto.request.SaveSceneDataRequestDTO;
import com.hztech.fastgpt.model.dto.response.ListAppResponseDTO;
import com.hztech.fastgpt.model.dto.response.ListCollectionResponseDTO;
import com.hztech.fastgpt.model.dto.response.ListDatasetResponseDTO;
import com.hztech.fastgpt.model.enums.EnumBusinessType;
import com.hztech.fastgpt.properties.FastGPTProperties;
import com.hztech.util.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.hztech.fastgpt.constant.ApiConstants.PATH_CHAT_COMPLETIONS;

/**
 * FastGPTService
 *
 * @author: boboo
 * @Date: 2024/8/27 11:45
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class FastGPTService {

    private final FastGPTProperties properties;

    private final ServiceRequests serviceRequests;

    private static final Cache<String, SaveSceneDataRequestDTO> CACHE = CacheUtil.newLFUCache(3000);

    public Boolean pushData(PushDataRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getType() == null || HzCollectionUtils.isEmpty(requestDTO.getData())) {
            return true;
        }
        log.info("推送{}数据{}条：{}", requestDTO.getType().getDesc(), requestDTO.getData().size(), JSONUtil.toJsonStr(requestDTO));
        int startPageNum = 1, startPageSize = 30;
        boolean hasMore = true;
        String datasetId = getDatasetId(requestDTO.getType());
        List<ListCollectionResponseDTO> collections = new ArrayList<>();
        // FastGPT中已存在的数据
        while (hasMore) {
            Map<String, Object> resultMap = serviceRequests.listCollection(datasetId, startPageNum, startPageSize, null, "");
            JSONObject jsonObject = JSONUtil.parseObj(resultMap).getJSONObject("data");
            collections.addAll(jsonObject.getBeanList("data", ListCollectionResponseDTO.class));
            hasMore = startPageNum++ * startPageSize < jsonObject.getInt("total");
        }

        Map<String, ListCollectionResponseDTO> map = HzStreamUtils.toIdentityMap(collections, ListCollectionResponseDTO::getName);
        for (PushDataRequestDTO.Data data : requestDTO.getData()) {
            if (map.containsKey(data.getId())) {
                // 包含该数据，查看更新时间是否一致
                ListCollectionResponseDTO responseDTO = map.get(data.getId());
                if (!data.getModifyTime().isEqual(responseDTO.getUpdateTime())) {
                    // 更新时间不一致
                    // 取业务系统的数据
                    // 1.删除旧数据
                    serviceRequests.deleteCollection(responseDTO.get_id());
                    // 2.插入新数据
                    insertData(datasetId, data);
                }
            } else {
                // 不包含该数据
                insertData(datasetId, data);
            }
        }

        List<String> deletedCollections = new HzCompareListUtils<>(requestDTO.getData().stream().map(PushDataRequestDTO.Data::getId).collect(Collectors.toList()), new ArrayList<>(map.keySet()), HzStringUtils::equals).getDelete();
        if (HzCollectionUtils.isNotEmpty(deletedCollections)) {
            log.info("准备删除{}个集合:{}", deletedCollections.size(), JSONUtil.toJsonStr(deletedCollections));
            deletedCollections.forEach(id -> serviceRequests.deleteCollection(map.get(id).get_id()));
        }
        return true;
    }

    private void insertData(String datasetId, PushDataRequestDTO.Data data) {
        // 创建集合
        Map<String, Object> resultMap = serviceRequests.createCollection(datasetId, null, data.getId(), "virtual", null);
        String collectionId = JSONUtil.parseObj(resultMap).getStr("data");
        // 插入新数据
        ListUtil.split(data.getData(), 200).forEach(list -> serviceRequests.pushData(collectionId, "chunk", null, list));
//        serviceRequests.pushData(collectionId, "chunk", null, data.getData());
    }

    private String getDatasetId(EnumBusinessType type) {
        return HzSpringUtils.getProperty("fastgpt.dataset." + type.name().toLowerCase());
//        return type.getDesc();
    }

    public Map<String, Object> chatCompletions(ChatCompletionsRequestDTO requestDTO) {
        MDC.put("chatKey", requestDTO.getChatKey());
        return serviceRequests.chatCompletions(requestDTO.getChatId(), requestDTO.getVariables(), requestDTO.getMessages());
    }

    @SneakyThrows
    public SseEmitter chatCompletionsStream(ChatCompletionsRequestDTO requestDTO) {
        MDC.put("chatKey", requestDTO.getChatKey());
        return chatCompletionsStream(requestDTO.getChatId(), requestDTO.getVariables(), requestDTO.getMessages());
//        return serviceRequests.chatCompletionsStream(requestDTO.getChatId(), requestDTO.getVariables(), requestDTO.getMessages());
    }

    /**
     * 流式对话
     *
     * @param chatId    为 undefined 时（不传入），不使用 FastGpt 提供的上下文功能，完全通过传入的 messages 构建上下文。 不会将你的记录存储到数据库中，你也无法在记录汇总中查阅到。
     *                  为非空字符串时，意味着使用 chatId 进行对话，自动从 FastGpt 数据库取历史记录，并使用 messages 数组最后一个内容作为用户问题。请自行确保 chatId 唯一，长度小于250，通常可以是自己系统的对话框ID。
     * @param variables 模块变量，一个对象，会替换模块中，输入框内容里的{{key}}
     * @param messages  结构与 GPT接口 chat模式一致。
     */
    public SseEmitter chatCompletionsStream(String chatId, Map<String, String> variables,
                                            List<ChatMessage> messages) {
//        if (log.isDebugEnabled()) {
        log.info("request chatCompletionsStream chatId={}, stream={}, variables={}, messages={}",
                chatId, true, variables, JSONUtil.toJsonStr(messages));
//        }
        JSONObject param = new JSONObject();
        param.set("chatId", chatId);
        param.set("stream", true);
        param.set("detail", false);
        param.set("variables", variables);
        param.set("messages", messages);

        try {
            return this.postSseRequest(PATH_CHAT_COMPLETIONS, param);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送sse请求
     *
     * @param path  请求路径
     * @param param 请求参数
     */
    private SseEmitter postSseRequest(String path, JSONObject param) {
        try {
            SseEmitter emitter = new SseEmitter();
            String chatKey = MDC.get("chatKey");
            String chatId = param.getStr("chatId");
            Mono.fromCallable(() -> {
                        AtomicBoolean firstBlankLine = new AtomicBoolean(false);
                        WebClient.create(combPath(path))
                                .post()
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.TEXT_EVENT_STREAM)
                                .body(BodyInserters.fromValue(param.toString()))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + StrUtil.blankToDefault(chatKey, this.properties.getChatKey()))
                                .retrieve()
                                .bodyToFlux(String.class)
                                .doOnNext(data -> {
                                    try {
                                        if (!firstBlankLine.get() && !HzStringUtils.equals("[DONE]", data) && JSONUtil.isTypeJSON(data) && HzStringUtils.equals(JSONUtil.getByPath(JSONUtil.parseObj(data), "choices[0].delta.content", ""), "\n")) {
                                            firstBlankLine.set(true);
                                        } else {
                                            if (HzStringUtils.equals("[DONE]", data)) {
                                                emitter.send(data);
                                            } else {
                                                JSONObject jsonObject = JSONUtil.parseObj(data);
                                                if (HzStringUtils.isNotBlank(data)) {
                                                    if (HzStringUtils.isNotBlank(chatId) && CACHE.containsKey(chatId) && JSONUtil.isTypeJSON(data)) {
                                                        SaveSceneDataRequestDTO requestDTO = CACHE.get(chatId);
                                                        jsonObject = JSONUtil.parseObj(data);
                                                        if (ObjectUtil.isNotEmpty(requestDTO.getData())) {
                                                            jsonObject.putOpt("data", requestDTO.getData());
                                                        }
                                                        if (HzStringUtils.isNotBlank(requestDTO.getCode())) {
                                                            jsonObject.putOpt("code", requestDTO.getCode());
                                                        }
                                                        if (ObjectUtil.isNotEmpty(requestDTO.getUserSelectData())) {
                                                            jsonObject.putOpt("userSelectData", requestDTO.getUserSelectData());
                                                        }
                                                    }
                                                }
                                                JSONObject delta = jsonObject.getByPath("choices[0].delta", JSONObject.class);
                                                if (delta == null || delta.isEmpty()) {
                                                    emitter.send(jsonObject.toString());
                                                } else {
                                                    String content = delta.getStr("content");
                                                    if (HzStringUtils.isNotBlank(content) && content.length() > 5) {
                                                        // 处理指定回复一次性返回太多文本
                                                        for (int i = 0; i < content.length(); i += 2) {
                                                            jsonObject.putByPath("choices[0].delta.content", StrUtil.sub(content, i, NumberUtil.min(i + 2, content.length())));
                                                            emitter.send(jsonObject.toString());
                                                            ThreadUtil.safeSleep(5);
                                                        }
                                                    } else {
                                                        emitter.send(jsonObject.toString());
                                                    }
                                                }
                                            }
                                        }
                                    } catch (IOException e) {
                                        log.error("Event Stream Exception:", e);
                                    }
                                })
                                .doOnError(error -> {
                                    log.error("Event Stream Error:", error);
                                    if (HzStringUtils.isNotBlank(chatId)) {
                                        CACHE.remove(chatId);
                                    }
                                })
                                .doOnComplete(() -> {
                                    emitter.complete();
                                    if (HzStringUtils.isNotBlank(chatId)) {
                                        CACHE.remove(chatId);
                                    }
                                })
                                .subscribe();

                        return emitter;
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
            return emitter;
        } catch (Exception e) {
            log.error("path=[{}], params=[{}] error.", path, param, e);
            throw new RuntimeException(e);
        }
    }

    private String combPath(String path) {
        return this.properties.getApiUrl() + path;
    }

    @SneakyThrows
    public Map<String, Object> uploadFile(String datasetId, String parentId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (!StrUtil.endWithAny(originalFilename, ".pdf", ".doc", ".docx", ".md", ".txt", ".html", ".csv")) {
            throw new HzRuntimeException("不支持的文件格式");
        }
        return serviceRequests.createFileCollection(file.getBytes(), originalFilename, datasetId, parentId, null, "chunk", 500, null, null);
    }

    @SneakyThrows
    public List<String> createFileCollection(String datasetId, String parentId, MultipartFile file) {
        List<String> collectionIds = new ArrayList<>();
        String originalFilename = file.getOriginalFilename();
        if (!StrUtil.endWithAny(originalFilename, ".pdf", ".doc", ".docx", ".md", ".txt", ".html", ".csv")) {
            throw new HzRuntimeException("不支持的文件格式");
        }
        doCreateFileCollection(file.getBytes(), originalFilename, datasetId, parentId, "chunk", collectionIds);
//        doCreateFileCollection(file.getBytes(), originalFilename, datasetId, parentId, "qa", collectionIds);
        return collectionIds;
    }

    private void doCreateFileCollection(byte[] bytes, String originalFilename, String datasetId, String parentId, String trainingType, List<String> collectionIds) {
        Map<String, Object> map = serviceRequests.createFileCollection(bytes, originalFilename, datasetId, parentId, null, trainingType, 500, null, null);
        String collectionId = JSONUtil.parseObj(map).getByPath("data.collectionId", String.class);
        if (HzStringUtils.isNotBlank(collectionId)) {
            collectionIds.add(collectionId);
        }
    }

    public Boolean deleteCollections(List<String> collectionIds) {
        Boolean result = Boolean.TRUE;
        for (String collectionId : collectionIds) {
            Map<String, Object> map = serviceRequests.deleteCollection(collectionId);
            JSONObject jsonObject = JSONUtil.parseObj(map);
            result = BooleanUtil.and(result, ObjectUtil.equals(jsonObject.get("code"), 200));
            log.info("删除集合{}：{}", collectionId, jsonObject);
        }
        return result;
    }

    public List<ListDatasetResponseDTO> listDataset(String parentId) {
        return JSONUtil.parseObj(serviceRequests.listDataset(parentId)).getBeanList("data", ListDatasetResponseDTO.class);
    }

    public List<ListAppResponseDTO> listApp(String parentId, String searchKey) {
        return JSONUtil.parseObj(serviceRequests.listApp(parentId, searchKey)).getBeanList("data", ListAppResponseDTO.class);
    }


    public void saveSceneData(SaveSceneDataRequestDTO requestDTO) {
        log.info("saveSceneData:{}", JSONUtil.toJsonStr(requestDTO));
        CACHE.put(requestDTO.getChatId(), requestDTO);
    }

    public Object getSceneData(String chatId) {
        return CACHE.get(chatId).getData();
    }

}
