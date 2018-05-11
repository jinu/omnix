package com.omnix.config;

import java.util.Locale;
import java.util.Properties;

import org.springframework.context.support.ReloadableResourceBundleMessageSource;

public class MessageSourceCustom extends ReloadableResourceBundleMessageSource {
    public Properties getMessageAll(Locale locale) {
        return getMergedProperties(locale).getProperties();
    }
    
    public String getMessage(String code, String defaultMessage, Locale locale) {
        return getMessage(code, null, defaultMessage, locale);
    }
    
    public String getMessage(String code, Locale locale) {
        return getMessage(code, null, "", locale);
    }
}
