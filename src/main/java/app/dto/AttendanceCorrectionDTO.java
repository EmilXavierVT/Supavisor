package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AttendanceCorrectionDTO {
    private Instant checkInAt;
    private Instant checkOutAt;
    private String reason;
}
