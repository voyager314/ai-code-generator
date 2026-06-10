package com.yzy.ai.parser;

public interface CodeParser<T> {
    T parse(String codeContent);
}
