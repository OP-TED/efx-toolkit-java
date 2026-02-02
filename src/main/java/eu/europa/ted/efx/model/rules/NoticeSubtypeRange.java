/*
 * Copyright 2022 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.model.rules;

import java.util.ArrayList;
import java.util.List;

import eu.europa.ted.efx.model.ParsedEntity;

public class NoticeSubtypeRange implements ParsedEntity, Iterable<String> {

    private final List<String> noticeSubtypes;

    public NoticeSubtypeRange(String rangeString, List<String> validNoticeSubtypes) {

        this.noticeSubtypes = new ArrayList<>();

        if (validNoticeSubtypes == null) {
            validNoticeSubtypes = List.of();
        }

        rangeString = (rangeString == null) ? "" : rangeString.trim();
        
        if (rangeString == "*" || rangeString.equalsIgnoreCase("ANY")) {
            noticeSubtypes.addAll(validNoticeSubtypes);
            return;
        }

        for (String item : rangeString.trim().split("\\s*,\\s*")) {
            if (item.isEmpty()) {
                continue;
            }

            String[] parts = item.split("\\s*-\\s*", -1);
            switch (parts.length) {
                case 1: {
                    int idx = validNoticeSubtypes.indexOf(parts[0]);
                    if (idx < 0) {
                        throw new IllegalArgumentException(
                                String.format("Invalid notice type ID '%s' in compressed list '%s'",
                                        parts[0], rangeString));
                    }
                    noticeSubtypes.add(validNoticeSubtypes.get(idx));
                    break;
                }
                case 2: {
                    int startIdx = validNoticeSubtypes.indexOf(parts[0]);
                    if (startIdx < 0) {
                        throw new IllegalArgumentException(
                                String.format("Invalid notice subtype '%s' in range '%s-%s'.",
                                        parts[0], parts[0], parts[1]));
                    }
                    int endIdx = validNoticeSubtypes.indexOf(parts[1]);
                    if (endIdx < 0) {
                        throw new IllegalArgumentException(
                                String.format("Invalid notice subtype '%s' in range '%s-%s'.",
                                        parts[1], parts[0], parts[1]));
                    }
                    if (startIdx > endIdx) {
                        throw new IllegalArgumentException(
                                String.format("Notice subtype range '%s-%s' is not in ascending order.", parts[0],
                                        parts[1]));
                    }

                    for (int i = startIdx; i <= endIdx; i++) {
                        noticeSubtypes.add(validNoticeSubtypes.get(i));
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException(
                            String.format("Invalid notice subtype token '%s'.",
                                    item));
            }
        }

    }

    public List<String> asList() {
        return List.copyOf(noticeSubtypes);
    }

    public int size() {
        return noticeSubtypes.size();
    }

    @Override
    public java.util.Iterator<String> iterator() {
        return noticeSubtypes.iterator();
    }
}
