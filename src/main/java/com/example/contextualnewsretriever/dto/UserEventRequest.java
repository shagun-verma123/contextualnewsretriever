package com.example.contextualnewsretriever.dto;

import com.example.contextualnewsretriever.enums.EventType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEventRequest {

    @NotBlank(message = "userId must not be blank")
    private String userId;

    @NotBlank(message = "articleId must not be blank")
    private String articleId;

    @NotNull(message = "eventType must be VIEW or CLICK")
    private EventType eventType;

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;
}
