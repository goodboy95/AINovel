package com.example.ainovel.dto;

import com.example.ainovel.model.ManuscriptSection;

import java.util.Map;

public class ManuscriptWithSectionsDto {
    private ManuscriptDto manuscript;
    /**
     * key = sceneId, value = active ManuscriptSection (latest version)
     */
    private Map<Long, ManuscriptSection> sections;

    public ManuscriptWithSectionsDto() {}

    public ManuscriptWithSectionsDto(ManuscriptDto manuscript, Map<Long, ManuscriptSection> sections) {
        this.manuscript = manuscript;
        this.sections = sections;
    }

    public ManuscriptDto getManuscript() {
        return manuscript;
    }

    public ManuscriptWithSectionsDto setManuscript(ManuscriptDto manuscript) {
        this.manuscript = manuscript;
        return this;
    }

    public Map<Long, ManuscriptSection> getSections() {
        return sections;
    }

    public ManuscriptWithSectionsDto setSections(Map<Long, ManuscriptSection> sections) {
        this.sections = sections;
        return this;
    }
}