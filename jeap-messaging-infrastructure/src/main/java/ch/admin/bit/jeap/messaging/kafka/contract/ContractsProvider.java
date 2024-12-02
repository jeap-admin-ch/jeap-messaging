package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.contract.v2.Contract;

import java.util.List;

public interface ContractsProvider {

    List<Contract> getContracts();

}
