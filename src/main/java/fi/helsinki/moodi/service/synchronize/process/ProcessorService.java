package fi.helsinki.moodi.service.synchronize.process;

import com.google.common.collect.Lists;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.enrich.EnrichException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Function;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Orchestrates processors.
 *
 * @see Processor
 * @see SynchronizationItem
 */
@Service
public class ProcessorService {

    private static final Logger LOGGER = getLogger(ProcessorService.class);

    private final Map<Action, Processor> processorsByAction;
    private final ProcessExecutor processExecutor;

    @Autowired
    public ProcessorService(List<Processor> processors, ProcessExecutor processExecutor) {
        this.processorsByAction = processors.stream()
                .collect(toMap(Processor::getAction, Function.identity(), (a, b) -> b));
        this.processExecutor = processExecutor;
    }

    private SynchronizationItem readItem(Future<SynchronizationItem> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new ProcessException("Error reading synchronization items from future", e);
        }
    }

    public List<SynchronizationItem> process(final List<SynchronizationItem> items) {
        final Map<Action, List<SynchronizationItem>> itemsByAction = groupItemsByAction(items);
        final  List<SynchronizationItem> processedItems = Lists.newArrayList();

        for (final Action action : Action.values()) {
            final List<SynchronizationItem> itemsToProcess = itemsByAction.getOrDefault(action, Lists.newArrayList());
            final Processor processor = processorsByAction.get(action);
            itemsToProcess.stream()
                .map(item -> processExecutor.processItem(item, processor))
                .map(this::readItem)
                .forEach(processedItems::add);
        }

        return processedItems;
    }

    private Map<Action, List<SynchronizationItem>> groupItemsByAction(final List<SynchronizationItem> items) {
        return items.stream().collect(groupingBy(ActionResolver::resolve));
    }
}
