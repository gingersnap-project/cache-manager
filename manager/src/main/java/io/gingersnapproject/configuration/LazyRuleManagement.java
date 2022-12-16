package io.gingersnapproject.configuration;

public interface LazyRuleManagement {
    void addRule(String name, Rule rule);
    void removeRule(String name);
}
