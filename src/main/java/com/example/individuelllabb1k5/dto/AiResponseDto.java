package com.example.individuelllabb1k5.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiResponseDto {

    @NotNull
    private String gameName;

    @NotNull
    @Size(min = 1)
    private List<String> good;

    @NotNull
    @Size(min = 1)
    private List<String> bad;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer score;

    @NotNull
    private String summary;

}
