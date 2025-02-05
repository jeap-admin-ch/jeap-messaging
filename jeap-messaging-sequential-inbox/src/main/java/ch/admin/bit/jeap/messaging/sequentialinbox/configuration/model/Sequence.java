package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import lombok.Getter;

import java.util.List;

@Getter
public class Sequence {

    private String type;

    private List<SequencedMessageType> messages;

    @Override
    public String toString() {
        return """
                Sequence{
                    type='%s',
                    messages=%s
                }
                """.formatted(type, messages);
    }

}
