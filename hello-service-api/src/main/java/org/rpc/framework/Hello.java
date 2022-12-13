package org.rpc.framework;

import lombok.*;

import java.io.Serializable;

/**
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Hello implements Serializable {
    private String message;
    private String description;
}
