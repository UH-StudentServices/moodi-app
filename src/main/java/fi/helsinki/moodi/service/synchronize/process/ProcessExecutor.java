package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

@Component
public class ProcessExecutor {

    @Async("taskExecutor")
    public Future<SynchronizationItem> processItem(SynchronizationItem item, Processor processor) {
        return new AsyncResult<>(processor.process(item));
    }

}
