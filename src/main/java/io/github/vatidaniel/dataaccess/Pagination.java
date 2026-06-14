package io.github.vatidaniel.dataaccess;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pagination {

    private long total;
    private int page;
    private int size;

    public int getTotalPage() {
        return size <= 0 ? 0 : (int) Math.ceil((double) total/size);
    }

}
