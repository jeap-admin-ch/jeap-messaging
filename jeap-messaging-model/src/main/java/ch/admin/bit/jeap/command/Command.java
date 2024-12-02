package ch.admin.bit.jeap.command;

import ch.admin.bit.jeap.messaging.model.Message;

public interface Command extends Message {
    String getCommandVersion();
}
