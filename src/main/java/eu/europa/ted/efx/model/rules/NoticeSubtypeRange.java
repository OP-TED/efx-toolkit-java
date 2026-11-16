/*
 * Copyright 2025 European Union
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
import java.util.HashSet;
import java.util.List;

import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.model.ParsedEntity;

public class NoticeSubtypeRange implements ParsedEntity, Iterable<String> {

    private final List<String> noticeSubtypes;
    private final boolean universal;

    public NoticeSubtypeRange(String rangeString, List<String> validNoticeSubtypesInAscendingOrder) {

        this.noticeSubtypes = new ArrayList<>();

        if (validNoticeSubtypesInAscendingOrder == null) {
            validNoticeSubtypesInAscendingOrder = List.of();
        }

        rangeString = (rangeString == null) ? "" : rangeString.trim();

        if (rangeString.equals("*") || rangeString.equalsIgnoreCase("ANY")) {
            this.noticeSubtypes.addAll(validNoticeSubtypesInAscendingOrder);
            this.universal = true;
            return;
        }

        for (String item : rangeString.trim().split("\\s*,\\s*")) {
            if (item.isEmpty()) {
                continue;
            }

            String[] parts = item.split("\\s*-\\s*", -1);
            switch (parts.length) {
                case 1: {
                    int idx = validNoticeSubtypesInAscendingOrder.indexOf(parts[0]);
                    if (idx < 0) {
                        throw SymbolResolutionException.unknownNoticeSubtype(parts[0]);
                    }
                    this.noticeSubtypes.add(validNoticeSubtypesInAscendingOrder.get(idx));
                    break;
                }
                case 2: {
                    int startIdx = validNoticeSubtypesInAscendingOrder.indexOf(parts[0]);
                    if (startIdx < 0) {
                        throw SymbolResolutionException.unknownNoticeSubtype(parts[0], parts[0] + "-" + parts[1]);
                    }
                    int endIdx = validNoticeSubtypesInAscendingOrder.indexOf(parts[1]);
                    if (endIdx < 0) {
                        throw SymbolResolutionException.unknownNoticeSubtype(parts[1], parts[0] + "-" + parts[1]);
                    }
                    if (startIdx > endIdx) {
                        throw InvalidUsageException.invalidNoticeSubtypeRangeOrder(parts[0], parts[1]);
                    }

                    for (int i = startIdx; i <= endIdx; i++) {
                        this.noticeSubtypes.add(validNoticeSubtypesInAscendingOrder.get(i));
                    }
                    break;
                }
                default:
                    throw InvalidUsageException.invalidNoticeSubtypeToken(item);
            }
        }

        this.universal = covers(this.noticeSubtypes, validNoticeSubtypesInAscendingOrder);
    }

    /**
     * The notice subtypes of the given ranges, in their order, as a list of the ranges separated by
     * commas gives them: the ranges {@code 1} and {@code 3-5} give the subtypes of {@code 1, 3-5}.
     *
     * @param ranges the ranges, each read on its own
     * @param validNoticeSubtypesInAscendingOrder every notice subtype, by which the result is
     *        universal when it holds them all
     */
    public NoticeSubtypeRange(List<NoticeSubtypeRange> ranges, List<String> validNoticeSubtypesInAscendingOrder) {
        this.noticeSubtypes = new ArrayList<>();
        for (NoticeSubtypeRange range : ranges) {
            this.noticeSubtypes.addAll(range.noticeSubtypes);
        }
        this.universal = covers(this.noticeSubtypes,
            validNoticeSubtypesInAscendingOrder == null ? List.of() : validNoticeSubtypesInAscendingOrder);
    }

    private static boolean covers(List<String> noticeSubtypes, List<String> validNoticeSubtypesInAscendingOrder) {
        return !validNoticeSubtypesInAscendingOrder.isEmpty()
            && new HashSet<>(noticeSubtypes).containsAll(validNoticeSubtypesInAscendingOrder);
    }

    public boolean isUniversal() {
        return this.universal;
    }

    public List<String> asList() {
        return List.copyOf(this.noticeSubtypes);
    }

    public int size() {
        return this.noticeSubtypes.size();
    }

    @Override
    public java.util.Iterator<String> iterator() {
        return this.noticeSubtypes.iterator();
    }
}
