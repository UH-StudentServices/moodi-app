package fi.helsinki.moodi.service.time;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class SystemTimeService implements TimeService {

    public LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public long getCurrentTimestamp() {
        return getCurrentDateTime().toInstant(ZoneOffset.UTC).getEpochSecond();
    }
}
