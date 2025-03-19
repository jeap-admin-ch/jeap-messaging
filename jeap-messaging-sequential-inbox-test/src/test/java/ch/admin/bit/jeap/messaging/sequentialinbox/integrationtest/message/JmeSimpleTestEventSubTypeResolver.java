package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SubTypeResolver;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;

public class JmeSimpleTestEventSubTypeResolver implements SubTypeResolver<JmeSimpleTestEvent, IceCreamFlavour> {

    @Override
    public IceCreamFlavour resolveSubType(JmeSimpleTestEvent event) {
        return IceCreamFlavour.valueOf(event.getPayload().getMessage());
    }
}
