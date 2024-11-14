package com.hztech.fastgpt.controller;

import com.hztech.fastgpt.model.dto.request.ChatCompletionsRequestDTO;
import com.hztech.fastgpt.model.dto.request.PushDataRequestDTO;
import com.hztech.fastgpt.model.dto.request.SaveSceneDataRequestDTO;
import com.hztech.fastgpt.model.dto.response.ListAppResponseDTO;
import com.hztech.fastgpt.model.dto.response.ListDatasetResponseDTO;
import com.hztech.fastgpt.service.FastGPTService;
import com.hztech.model.dto.HzResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * FastGPTController
 *
 * @author: boboo
 * @Date: 2024/8/27 10:13
 **/
@RestController
@RequiredArgsConstructor
public class FastGPTController {

    private final FastGPTService fastGPTService;

    /**
     * 业务数据推送
     *
     * @param requestDTO 请求参数
     */
    @PostMapping("/api/v1/pushData")
    public HzResponse<Boolean> pushData(@RequestBody PushDataRequestDTO requestDTO) {
        return HzResponse.success(fastGPTService.pushData(requestDTO));
    }

    /**
     * 创建文件集合
     *
     * @param datasetId 知识库id
     * @param parentId  文件夹id
     * @param file      文件
     */
    @PostMapping("/api/v1/uploadFile")
    public HzResponse<Map<String, Object>> uploadFile(@RequestParam("datasetId") String datasetId,
                                                      @RequestParam(value = "parentId", required = false) String parentId,
                                                      @RequestPart("file") MultipartFile file) {
        return HzResponse.success(fastGPTService.uploadFile(datasetId, parentId, file));
    }

    /**
     * 创建文件集合
     *
     * @param datasetId 知识库id
     * @param parentId  文件夹id
     * @param file      文件
     */
    @PostMapping("/api/v1/createFileCollection")
    public HzResponse<List<String>> createFileCollection(@RequestParam("datasetId") String datasetId,
                                                         @RequestParam(value = "parentId", required = false) String parentId,
                                                         @RequestPart("file") MultipartFile file) {
        return HzResponse.success(fastGPTService.createFileCollection(datasetId, parentId, file));
    }

    /**
     * 删除文件集合
     *
     * @param collectionIds 集合id
     */
    @PostMapping("/api/v1/deleteCollections")
    public HzResponse<Boolean> deleteCollections(@RequestBody List<String> collectionIds) {
        return HzResponse.success(fastGPTService.deleteCollections(collectionIds));
    }

    /**
     * 查询知识库列表（开源版最多30个知识库）
     *
     * @param parentId 文件夹id
     */
    @GetMapping("/api/v1/dataset/list")
    public HzResponse<List<ListDatasetResponseDTO>> listDataset(@RequestParam(value = "parentId", required = false) String parentId) {
        return HzResponse.success(fastGPTService.listDataset(parentId));
    }

    /**
     * 查询应用列表
     *
     * @param parentId  文件夹id
     * @param searchKey 搜索关键字
     */
    @GetMapping("/api/v1/app/list")
    public HzResponse<List<ListAppResponseDTO>> listApp(@RequestParam(value = "parentId", required = false) String parentId,
                                                        @RequestParam(value = "searchKey", required = false) String searchKey) {
        return HzResponse.success(fastGPTService.listApp(parentId, searchKey));
    }

    /**
     * 大模型聊天对话
     *
     * @param requestDTO 请求参数
     */
    @PostMapping("/api/v1/chat/completions")
    public Map<String, Object> chatCompletions(@RequestBody ChatCompletionsRequestDTO requestDTO) {
        return fastGPTService.chatCompletions(requestDTO);
    }

    /**
     * 大模型聊天对话
     *
     * @param requestDTO 请求参数
     */
    @PostMapping(value = "/api/v1/chat/completions/stream")
    public SseEmitter chatCompletionsStream(@RequestBody ChatCompletionsRequestDTO requestDTO) {
        return fastGPTService.chatCompletionsStream(requestDTO);
    }

    /**
     * 保存聊天场景code以及响应数据
     */
    @PostMapping("/api/v1/chat/saveSceneData")
    public HzResponse<Void> saveSceneData(@RequestBody SaveSceneDataRequestDTO requestDTO) {
        fastGPTService.saveSceneData(requestDTO);
        return HzResponse.success();
    }

    @GetMapping("/api/v1/chat/getSceneData")
    public HzResponse<Object> getSceneData(@RequestParam("chatId") String chatId) {
        return HzResponse.success(fastGPTService.getSceneData(chatId));
    }
}
