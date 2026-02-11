package com.dynamicform.form.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for promoting a WIP version to a new committed version.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoteVersionRequest {

    private String changeDescription;
}
