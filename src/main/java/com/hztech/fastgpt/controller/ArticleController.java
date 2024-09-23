package com.hztech.fastgpt.controller;

import com.hztech.fastgpt.service.IArticleService;
import com.hztech.model.dto.HzResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ArticleController
 *
 * @author: boboo
 * @Date: 2024/9/5 14:02
 **/
@RestController
@RequiredArgsConstructor
public class ArticleController {

    private final IArticleService articleService;

    @PostMapping("/api/v1/article/save")
    public HzResponse<Boolean> articleSave(@RequestParam(value = "from", defaultValue = "100") Integer from, @RequestParam(value = "size", defaultValue = "100") Integer size) {
        return HzResponse.success(articleService.save(from, size));
    }
}
