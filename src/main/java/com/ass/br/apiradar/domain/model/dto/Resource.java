package com.ass.br.apiradar.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Resource {
    private String dataset;
    private String planet;

    @Override
    public String toString() {
        return "Resource{" +
                "dataset='" + dataset + '\'' +
                ", planet='" + planet + '\'' +
                '}';
    }
}
