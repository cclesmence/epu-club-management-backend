package com.sep490.backendclubmanagement.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class PageableRequest {
    public Integer page;
    public Integer size;
    public List<String> sort;
    public String keyword;

    @JsonIgnore
    public Pageable getPageable() {
        return getPageable(null);
    }

    @JsonIgnore
    public Pageable getPageable(String defaultSort) {
        if ((this.sort == null || this.sort.isEmpty())  && !StringUtils.isEmpty(defaultSort)) {
            this.sort = List.of(defaultSort);
        }
        if(this.sort == null || this.sort.isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(getPage() - 1, getSize());
        }
        return org.springframework.data.domain.PageRequest.of(getPage() - 1, getSize(), getSortObj());
    }
    @JsonIgnore
    @JsonIgnoreProperties
    public Sort getSortObj() {
        validSort();
        List<Sort.Order> sort = this.sort.stream().map(s -> {
            String[] parts = s.split(",");
            return new Sort.Order(Sort.Direction.fromString(parts[1]), parts[0]);
        }).toList();
        return Sort.by(sort);
    }
    private void validSort(){
        sort = sort.stream()
                .map(s -> s.replaceAll(" ", ""))
                .collect(Collectors.toList());
    }

    public Integer getPage() {
        return Objects.nonNull(page) ? page : 1;
    }
    public Integer getSize() {
        // If pageSize is 0, fetch all records
        return Objects.nonNull(size) && size != 0 ? size : Integer.MAX_VALUE;
    }

    public String getKeyword() {
        if (keyword != null) {
            return keyword.trim();
        }
        return null;
    }
}
