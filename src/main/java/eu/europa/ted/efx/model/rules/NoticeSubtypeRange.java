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

/**
 * The notice subtypes that the {@code IN} clause of a validation rule names, in the order it names
 * them, and whether they are every notice subtype.
 */
public class NoticeSubtypeRange implements ParsedEntity, Iterable<String> {

    private final List<String> noticeSubtypes;
    private final boolean universal;

    private NoticeSubtypeRange(List<String> noticeSubtypes, boolean universal) {
        this.noticeSubtypes = List.copyOf(noticeSubtypes);
        this.universal = universal;
    }

    /**
     * Every notice subtype, as {@code IN *} and {@code IN ANY} name them.
     *
     * @param knownNoticeSubtypes the notice subtypes of the SDK, in ascending order
     * @return the notice subtypes, universal
     */
    public static NoticeSubtypeRange all(List<String> knownNoticeSubtypes) {
        return new NoticeSubtypeRange(knownNoticeSubtypes, true);
    }

    public boolean isUniversal() {
        return this.universal;
    }

    public List<String> asList() {
        return this.noticeSubtypes;
    }

    public int size() {
        return this.noticeSubtypes.size();
    }

    @Override
    public java.util.Iterator<String> iterator() {
        return this.noticeSubtypes.iterator();
    }

    /**
     * Collects the notice subtypes of an {@code IN} list, one item of the list at a time: a notice
     * subtype, as in {@code 1}, or a range of them, as in {@code 1-3}.
     */
    public static final class Builder {

        private final List<String> knownNoticeSubtypes;
        private final List<String> noticeSubtypes = new ArrayList<>();

        /**
         * @param knownNoticeSubtypes the notice subtypes of the SDK, in ascending order
         */
        public Builder(List<String> knownNoticeSubtypes) {
            this.knownNoticeSubtypes = knownNoticeSubtypes;
        }

        /**
         * Adds a notice subtype.
         *
         * @param noticeSubtype the notice subtype, as written
         * @return this builder
         * @throws SymbolResolutionException if the SDK has no such notice subtype
         */
        public Builder add(String noticeSubtype) {
            if (!this.knownNoticeSubtypes.contains(noticeSubtype)) {
                throw SymbolResolutionException.unknownNoticeSubtype(noticeSubtype);
            }
            this.noticeSubtypes.add(noticeSubtype);
            return this;
        }

        /**
         * Adds the notice subtypes from one notice subtype to another, in ascending order.
         *
         * @param start the first notice subtype, as written
         * @param end the last notice subtype, as written
         * @return this builder
         * @throws SymbolResolutionException if the SDK has no such notice subtype as one of the two
         * @throws InvalidUsageException if the first notice subtype comes after the last
         */
        public Builder add(String start, String end) {
            int startIndex = this.knownNoticeSubtypes.indexOf(start);
            if (startIndex < 0) {
                throw SymbolResolutionException.unknownNoticeSubtype(start, start + "-" + end);
            }
            int endIndex = this.knownNoticeSubtypes.indexOf(end);
            if (endIndex < 0) {
                throw SymbolResolutionException.unknownNoticeSubtype(end, start + "-" + end);
            }
            if (startIndex > endIndex) {
                throw InvalidUsageException.invalidNoticeSubtypeRangeOrder(start, end);
            }
            this.noticeSubtypes.addAll(this.knownNoticeSubtypes.subList(startIndex, endIndex + 1));
            return this;
        }

        /**
         * @return the notice subtypes added, universal if they include every notice subtype of the
         *         SDK
         */
        public NoticeSubtypeRange build() {
            return new NoticeSubtypeRange(this.noticeSubtypes, !this.knownNoticeSubtypes.isEmpty()
                && new HashSet<>(this.noticeSubtypes).containsAll(this.knownNoticeSubtypes));
        }
    }
}
