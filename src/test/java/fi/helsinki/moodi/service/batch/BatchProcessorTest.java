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


import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertTrue;

public class BatchProcessorTest {

    private static final int ITEM_COUNT = 100;
    private static final int BATCH_SIZE = 30;

    @Test
    public void thatAllBatchItemsAreProcessedOnce() {

        List<BatchItem> batchItems = createBatchItems(ITEM_COUNT);

        BatchProcessor<BatchItem> batchProcessor = new BatchProcessor<>();

        List<BatchItem> processedItems = batchProcessor.process(batchItems, this::processBatch, BATCH_SIZE);

        assertTrue(processedItems.stream().allMatch(BatchItem::isProcessed));
        assertTrue(processedItems.stream().allMatch(item -> item.getProcessedCount() == 1));
    }

    private List<BatchItem> processBatch(List<BatchItem> itemsToProcess) {
        itemsToProcess.stream()
            .forEach(item -> item.setProcessed(true));

        return itemsToProcess;
    }

    private List<BatchItem> createBatchItems(final int numItems) {
        List<BatchItem> batchItems = newArrayList();

        for(int i = 0; i < numItems; i++) {
            batchItems.add(new BatchItem(false));
        }

        return batchItems;
    }

    private static class BatchItem {
        private int processedCount = 0;
        private boolean isProcessed;

        public BatchItem(boolean isProcessed) {
            this.isProcessed = isProcessed;
        }

        public boolean isProcessed() {
            return isProcessed;
        }

        public void setProcessed(boolean processed) {
            isProcessed = processed;
            processedCount++;
        }

        public int getProcessedCount() {
            return processedCount;
        }
    }

}
