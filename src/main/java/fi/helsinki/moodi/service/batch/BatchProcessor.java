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

package fi.helsinki.moodi.service.batch;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BatchProcessor<T> {

    private static final Logger LOGGER = getLogger(BatchProcessor.class);

    public interface ProcessBatch<T> {
        List<T> apply(List<T> currentItems);
    }

    public List<T> process(final List<T> items,
                           final ProcessBatch processBatch,
                           final int batchSize) {
        return process(items, processBatch, newArrayList(), newArrayList(), batchSize);
    }

    private List<T> process(final List<T> items,
                            final ProcessBatch processBatch,
                            final List<T> results,
                            final List<T> processedItems,
                            final int batchSize) {

        List<T> itemsToProcess = items
            .stream()
            .filter(item -> !processedItems.contains(item))
            .limit(batchSize)
            .collect(Collectors.toList());

        if (itemsToProcess.size() > 0) {
            LOGGER.info("Processing batch of {} items", itemsToProcess.size());
            results.addAll(processBatch.apply(itemsToProcess));
            processedItems.addAll(itemsToProcess);
            return process(items, processBatch, results, processedItems, batchSize);
        } else {
            return results;
        }

    }
}
