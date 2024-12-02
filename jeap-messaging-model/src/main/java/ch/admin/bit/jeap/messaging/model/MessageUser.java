package ch.admin.bit.jeap.messaging.model;

import java.util.Map;

public interface MessageUser {

    String getId();

    String getGivenName();

    String getFamilyName();

    String getBusinessPartnerName();

    String getBusinessPartnerId();

    Map<String, String> getPropertiesMap();

}
