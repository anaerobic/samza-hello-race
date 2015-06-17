/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package race.task;

import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemStream;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.StreamTask;
import org.apache.samza.task.TaskCoordinator;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RaceCheckpointCheckerStreamTask implements StreamTask {
    private static final Logger log = LoggerFactory.getLogger(RaceCheckpointCheckerStreamTask.class);

    private static SystemStream OUTPUT_STREAM = new SystemStream("kafka", "race-checkpoint-checks");

    private Map<Integer, Map<Integer, String>> bibChecks = new HashMap<Integer, Map<Integer, String>>();
    private Map<String, Object> checks = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    @Override
    public void process(IncomingMessageEnvelope envelope, MessageCollector collector, TaskCoordinator coordinator) {

        Map<String, Object> msgBibCheck = (Map<String, Object>) envelope.getMessage();

        Integer bib = (Integer) msgBibCheck.get("bib");

        Map<String, String> checkpoints = (Map<String, String>) msgBibCheck.get("checkpoints");

        Map<Integer, String> converted = new HashMap<Integer, String>();
        for (Map.Entry<String, String> meta : checkpoints.entrySet()) {

            log.info(meta.toString());

            converted.put(Integer.parseInt(meta.getKey()), meta.getValue());
        }

        bibChecks.put(bib, converted);

        // iterate over all of the checkpoints we have received for this bib and
        // validate that they are consistent with previous checkpoints
        Set<Map.Entry<Integer, String>> invalidChecks = null;
        Map.Entry<Integer, String> lastCheck = null;
        for (Map.Entry<Integer, String> check : bibChecks.get(bib).entrySet()) {

            // each successive check should have:
            // the next checkpoint, and
            // a higher time value

            if (lastCheck != null &&
                    ((check.getKey() - lastCheck.getKey() != 1) ||
                            LocalTime.parse(check.getValue())
                                    .isBefore(LocalTime.parse(lastCheck.getValue())))) {
                if (invalidChecks == null) {
                    invalidChecks = new HashSet<Map.Entry<Integer, String>>();
                }
                invalidChecks.add(check);
            }

            lastCheck = check;
        }

        checks.put("bib", bib);
        checks.put("bib-is-valid", invalidChecks == null ? "Yes" : "No");
        checks.put("checkpoints-passed", bibChecks.get(bib).size());

        if (invalidChecks != null) {
            checks.put("invalid-checkpoints", invalidChecks);
        }

        collector.send(new OutgoingMessageEnvelope(OUTPUT_STREAM, checks));
    }
}
