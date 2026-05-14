package com.safeoutput.core;

public interface SafeOutputMaskService {

    String mask(String value, String type);

    Object maskObject(Object value);

    String maskStrong(String value);

    Object maskObjectStrong(Object value);
}
