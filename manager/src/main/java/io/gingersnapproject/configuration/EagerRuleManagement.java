package io.gingersnapproject.configuration;

public interface EagerRuleManagement {
    void addRule(String name, Rule rule);
    void removeRule(String name);
}
