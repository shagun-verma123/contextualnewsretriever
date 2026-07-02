package com.example.contextualnewsretriever.dto;

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
public class ImportResultResponse {
    private int totalRecordsRead;
    private int inserted;
    private int skippedDuplicates;
}
