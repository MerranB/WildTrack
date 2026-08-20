package com.wildtrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "movebank")
public class MovebankProperties {

    private List<Long> studyIds;

    public List<Long> getStudyIds() {
        return studyIds;
    }

    public void setStudyIds(List<Long> studyIds) {
        this.studyIds = studyIds;
    }
}
