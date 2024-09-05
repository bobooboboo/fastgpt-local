package com.hztech.fastgpt.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hztech.exception.HzRuntimeException;
import com.hztech.fastgpt.ServiceRequests;
import com.hztech.fastgpt.model.dto.request.ChatCompletionsRequestDTO;
import com.hztech.fastgpt.model.dto.request.PushDataRequestDTO;
import com.hztech.fastgpt.model.dto.response.ListAppResponseDTO;
import com.hztech.fastgpt.model.dto.response.ListCollectionResponseDTO;
import com.hztech.fastgpt.model.dto.response.ListDatasetResponseDTO;
import com.hztech.fastgpt.model.enums.EnumBusinessType;
import com.hztech.util.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final ServiceRequests serviceRequests;

    public Boolean pushData(PushDataRequestDTO requestDTO) {
        if (requestDTO == null || HzCollectionUtils.isEmpty(requestDTO.getData())) {
            return true;
        }

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
            log.info("准备删除{}个集合", deletedCollections.size());
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
        return serviceRequests.chatCompletionsStream(requestDTO.getChatId(), requestDTO.getVariables(), requestDTO.getMessages());
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
}
