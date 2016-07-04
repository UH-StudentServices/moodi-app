package fi.helsinki.moodi.service.time;

import java.time.LocalDateTime;

public interface TimeService {

    LocalDateTime getCurrentDateTime();

    long getCurrentTimestamp();
}
