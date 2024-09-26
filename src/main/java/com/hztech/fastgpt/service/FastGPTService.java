package com.hztech.fastgpt.service;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
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
        serviceRequests.pushData(collectionId, "chunk", null, data.getData());
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
     * @param chatId       为 undefined 时（不传入），不使用 FastGpt 提供的上下文功能，完全通过传入的 messages 构建上下文。 不会将你的记录存储到数据库中，你也无法在记录汇总中查阅到。
     *                     为非空字符串时，意味着使用 chatId 进行对话，自动从 FastGpt 数据库取历史记录，并使用 messages 数组最后一个内容作为用户问题。请自行确保 chatId 唯一，长度小于250，通常可以是自己系统的对话框ID。
     * @param variables    模块变量，一个对象，会替换模块中，输入框内容里的{{key}}
     * @param messages     结构与 GPT接口 chat模式一致。
     */
    public SseEmitter chatCompletionsStream(String chatId, Map<String, String> variables,
                                            List<ChatMessage> messages) {
        if (log.isDebugEnabled()) {
            log.debug("request chatCompletionsStream chatId={}, stream={}, variables={}, messages={}",
                    chatId, true, variables, messages);
        }
        JSONObject param = new JSONObject();
        param.set("chatId", chatId);
        param.set("stream", true);
        param.set("detail", false);
        param.set("variables", variables);
        param.set("messages", messages);

        try {
            return this.postSSERequest(PATH_CHAT_COMPLETIONS, param);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    private SseEmitter postSSERequest(String path, JSONObject param) {
        try {
            SseEmitter emitter = new SseEmitter();
            String chatKey = MDC.get("chatKey");
            String chatId = param.getStr("chatId");
            Mono.fromCallable(() -> {
                        log.info("sse start:{}", DateUtil.now());
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
                                        if (HzStringUtils.isNotBlank(data)) {
                                            if (HzStringUtils.isNotBlank(chatId) && CACHE.containsKey(chatId) && !HzStringUtils.equals("[DONE]", data) && JSONUtil.isTypeJSON(data)) {
                                                SaveSceneDataRequestDTO requestDTO = CACHE.get(chatId);
                                                JSONObject jsonObject = JSONUtil.parseObj(data);
                                                if (ObjectUtil.isNotEmpty(requestDTO.getData())) {
                                                    jsonObject.set("data", requestDTO.getData());
                                                }
                                                if (HzStringUtils.isNotBlank(requestDTO.getCode())) {
                                                    jsonObject.set("code", requestDTO.getCode());
                                                }
                                                data = jsonObject.toString();
                                            }
                                            emitter.send(data);
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
                                    log.info("sse finish:{}", DateUtil.now());
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
            log.info("path=[{}], params=[{}] error.", path, param, e);
            throw new RuntimeException(e);
        }
    }

    private String combPath(String path) {
        return this.properties.getApiUrl() + path;
    }

    @SneakyThrows
    public Boolean uploadFile(String datasetId, String parentId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (!StrUtil.endWithAny(originalFilename, ".pdf", ".doc", ".docx", ".md", ".txt", ".html", ".csv")) {
            throw new HzRuntimeException("不支持的文件格式");
        }
        serviceRequests.createFileCollection(file.getBytes(), originalFilename, datasetId, parentId, null, "chunk", 500, null, null);
        return true;
    }

    public List<ListDatasetResponseDTO> listDataset(String parentId) {
        return JSONUtil.parseObj(serviceRequests.listDataset(parentId)).getBeanList("data", ListDatasetResponseDTO.class);
    }

    public List<ListAppResponseDTO> listApp(String parentId, String searchKey) {
        return JSONUtil.parseObj(serviceRequests.listApp(parentId, searchKey)).getBeanList("data", ListAppResponseDTO.class);
    }


    public void saveSceneData(SaveSceneDataRequestDTO requestDTO) {
        CACHE.put(requestDTO.getChatId(), requestDTO);
    }
}
