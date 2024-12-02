package ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing;

import lombok.*;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings("JpaDataSourceORMInspection")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED) // for JPA
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Getter
@Entity
@Table(name = "idempotent_processing")
public class IdempotentProcessing {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private IdempotentProcessingIdentity id;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

}


