package com.cg.exception;

import lombok.*;
import lombok.Builder.Default;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ErrorDto {

    @Default
    private boolean error = true;
    private String message;

}
