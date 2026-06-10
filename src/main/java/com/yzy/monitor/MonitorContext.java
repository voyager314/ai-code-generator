package com.yzy.monitor;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitorContext implements Serializable {
    private String appId;
    private String userId;
    @Serial
    private static final long serialVersionUID = 1L;
}
