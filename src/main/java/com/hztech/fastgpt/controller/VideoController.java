package com.hztech.fastgpt.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * VideoController
 *
 * @author: boboo
 * @Date: 2024/9/19 18:59
 **/
@Slf4j
@RestController
@RequiredArgsConstructor
public class VideoController {

    @SneakyThrows
    @PostMapping("/api/v1/image2video")
    public String image2video(@RequestParam(value = "url", required = false) String url) {
        JSONArray jsonArray = JSONUtil.parseArray(url);
        String tempDir = RandomUtil.randomString(20);
//        File file = FileUtil.touch("/root/ffmpeg/" + tempDir);
        List<File> tempFiles = new ArrayList<>(jsonArray.size());
        for (int i = 1; i <= jsonArray.size(); i++) {
            String fileUrl = jsonArray.get(i - 1, String.class);
            File tempFile = FileUtil.touch(StrUtil.format("/root/ffmpeg/{}.jpeg", i));
            FileUtil.writeBytes(HttpUtil.downloadBytes(fileUrl), tempFile);
            tempFiles.add(tempFile);
        }
        String command = StrUtil.format("docker exec app_ffmpeg ffmpeg -framerate 0.33 -f image2 -i /mnt/app/%d.jpeg -c:v libx264 -pix_fmt yuv420p -r 15 /mnt/app/video/{}.mp4", tempDir);
        Process process = RuntimeUtil.exec(command);
        process.waitFor();
        String result = RuntimeUtil.getResult(process);
        log.info("生成视频结果：{}", result);
        tempFiles.forEach(FileUtil::del);
//        FileUtil.del(file);
        String moveResult = RuntimeUtil.execForStr("mv /root/ffmpeg/video/" + tempDir + ".mp4" + " /fstore/" + tempDir + ".mp4");
        log.info("移动视频结果：{}", moveResult);
        return "http://192.168.1.13:8080/fstore/" + tempDir + ".mp4";
    }
}
