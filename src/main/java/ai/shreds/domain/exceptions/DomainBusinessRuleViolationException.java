package ai.shreds.domain.exceptions;

import java.util.Map;

public class DomainBusinessRuleViolationException extends RuntimeException {
    private final String rule;
    private final Map<String, Object> details;

    public DomainBusinessRuleViolationException(String rule, Map<String, Object> details) {
        super(rule);
        this.rule = rule;
        this.details = details;
    }

    public String getRule() {
        return rule;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
