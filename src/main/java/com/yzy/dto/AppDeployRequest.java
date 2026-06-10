package com.yzy.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppDeployRequest implements Serializable {
    private Long appId;
    @Serial
    private static final long serialVersionUID = 1L;
}
