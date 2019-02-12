package uk.gov.hmcts.reform.em.orchestrator.service.ccdcallbackhandler;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class CcdCallbackResponseDto {

    private JsonNode copyOfCcdData;

    public CcdCallbackResponseDto() {
    }

    public CcdCallbackResponseDto(JsonNode caseData) {
        this.copyOfCcdData = caseData != null ? caseData.deepCopy() : null;
        setData(caseData);
    }

    private JsonNode data;

    private List<String> errors = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();

    public JsonNode getData() {
        return CollectionUtils.isNotEmpty(getErrors()) ?  copyOfCcdData : data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
