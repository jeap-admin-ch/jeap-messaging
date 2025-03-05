package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

record CountByType(String messageType, double waitingCount) {
}
