package com.safeoutput.core;

public interface SafeOutputMaskService {

    String mask(String value, String type);

    Object maskObject(Object value);
}
