package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글 작성/수정 요청 DTO (Bean Validation 적용)
 */
@Getter
@Setter
@NoArgsConstructor
public class PostWriteRequestDto {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;
}
