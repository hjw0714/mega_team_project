package com.application.foodhub.post;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class PostDTO {
    private Long postId = null;
    private String userId;
    private String nickname;
    private Long categoryId;
    private Long subCateId;
    private String cateNm;
    private String subCateNm;
    private String title;
    private String content;
    private Long viewCnt;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private Date createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private Date updatedAt;
    
    
}