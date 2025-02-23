package com.ass.br.apiradar.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class NasaImageResponse {
    private String date;
    private String id;
    private Resource resource;

    @JsonProperty("service_version")
    private String serviceVersion;

    private String url;

    @Override
    public String toString() {
        return "NasaImageResponse{" +
                "date='" + date + '\'' +
                ", id='" + id + '\'' +
                ", resource=" + resource +
                ", serviceVersion='" + serviceVersion + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}

