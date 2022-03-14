/*
 * This file is part of Moodi application.
 *
 * Moodi application is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Moodi application is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Moodi application.  If not, see <http://www.gnu.org/licenses/>.
 */

package fi.helsinki.moodi.service.synchronize.process;

import com.google.common.collect.Lists;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * Orchestrates processors.
 *
 * @see Processor
 * @see SynchronizationItem
 */
@Service
public class ProcessorService {

    private final Map<Action, Processor> processorsByAction;
    private final ProcessExecutor processExecutor;

    @Autowired
    public ProcessorService(List<Processor> processors, ProcessExecutor processExecutor) {
        this.processorsByAction = processors.stream()
                .collect(toMap(Processor::getAction, Function.identity(), (a, b) -> b));
        this.processExecutor = processExecutor;
    }

    public List<SynchronizationItem> process(final List<SynchronizationItem> items) {
        final Map<Action, List<SynchronizationItem>> itemsByAction = groupItemsByAction(items);
        final  List<SynchronizationItem> processedItems = Lists.newArrayList();

        for (final Action action : Action.values()) {
            final List<SynchronizationItem> itemsToProcess = itemsByAction.getOrDefault(action, Lists.newArrayList());
            final Processor processor = processorsByAction.get(action);
            itemsToProcess.stream()
                .map(item -> processExecutor.processItem(item, processor))
                .forEach(processedItems::add);
        }

        return processedItems;
    }

    private Map<Action, List<SynchronizationItem>> groupItemsByAction(final List<SynchronizationItem> items) {
        return items.stream().collect(groupingBy(ActionResolver::resolve));
    }
}
