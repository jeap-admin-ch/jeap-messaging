package ch.admin.bit.jeap.messaging.contract.plugin.publish.dto;

import lombok.Value;

import java.util.List;

@Value
public class CreateMessageContractsDto {

    List<MessageContractDto> contracts;
}
