package io.github.vatidaniel.dataaccess;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RowStruct implements KeyValueFormat<Integer, String> {

    private Integer index;
    private String value;

}
