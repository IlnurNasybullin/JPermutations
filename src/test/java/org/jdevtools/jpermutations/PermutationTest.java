/*
 * Copyright 2021 Ilnur Nasybullin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jdevtools.jpermutations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Class for testing construction of {@link Permutation} object.
 */
public class PermutationTest {

    @ParameterizedTest
    @MethodSource("_unorderedPermutationConstruct_Exception")
    public <T, X extends Exception> void unorderedPermutationConstruct_Exception(Set<T>[] elements, Class<T>[] elementTypes,
                                                                                 Class<X>[] expectedExceptions) {
        int[] i = {0};

        Class<X> expectedException;
        for (; i[0] < elements.length; i[0]++) {
            expectedException = expectedExceptions[i[0]];
            Assertions.assertThrows(expectedException, () -> Permutation.unordered(elements[i[0]], elementTypes[i[0]]));
        }
    }

    public static Stream<Arguments> _unorderedPermutationConstruct_Exception() {
        Set<?>[] elements = new Set[]{
                null, Set.of(), Set.of(), Set.of("1", "2", "3")
        };

        Class<?>[] elementTypes = new Class[]{
                Integer.class, null, Void.TYPE, Integer.class
        };

        Class<? extends Exception>[] expectedExceptions = new Class[] {
                NullPointerException.class, NullPointerException.class, IllegalArgumentException.class, ArrayStoreException.class
        };

        return Stream.of(Arguments.of(
            elements, elementTypes, expectedExceptions
        ));
    }

    @ParameterizedTest
    @MethodSource("_orderedPermutationConstructWithSet_Exception")
    public <T, X extends Exception> void orderedPermutationConstructWithSet_Exception(Set<T>[] elements, Class<T>[] elementTypes,
                                                                                      Comparator<T>[] comparators,
                                                                                      Class<X>[] expectedExceptions) {
        int[] i = {0};

        Class<X> expectedException;
        for (; i[0] < elements.length; i[0]++) {
            expectedException = expectedExceptions[i[0]];
            Assertions.assertThrows(expectedException,
                    () -> Permutation.ordered(elements[i[0]], elementTypes[i[0]], comparators[i[0]]));
        }
    }

    public static Stream<Arguments> _orderedPermutationConstructWithSet_Exception() {
        Set<?>[] elements = new Set[]{
                null, Set.of(),  Set.of("1", "2", "3"), Set.of(), Set.of(Integer.class, String.class)
        };

        Class<?>[] elementTypes = new Class[]{
                Integer.class, null, Integer.class, Void.TYPE, Class.class
        };

        Comparator<?>[] comparators = new Comparator[] {
                null, null, null, null, null
        };

        Class<? extends Exception>[] expectedExceptions = new Class[] {
                NullPointerException.class, NullPointerException.class, ArrayStoreException.class,
                IllegalArgumentException.class, IllegalArgumentException.class
        };

        return Stream.of(Arguments.of(
                elements, elementTypes, comparators, expectedExceptions
        ));
    }

    @ParameterizedTest
    @MethodSource({
            "_orderedPermutationsConstructWithSortedSet"
    })
    public <T, X extends Exception> void orderedPermutationConstructWithSortedSet_Exception(SortedSet<T>[] elements,
                                                                                            Class<T>[] elementTypes,
                                                                                            Class<X>[] expectedExceptions) {
        for (int[] i = {0}; i[0] < elements.length; i[0]++) {
            Assertions.assertThrows(expectedExceptions[i[0]], () -> Permutation.ordered(elements[i[0]], elementTypes[i[0]]));
        }
    }

    public static Stream<Arguments> _orderedPermutationsConstructWithSortedSet() {
        SortedSet<?>[] elements = new SortedSet[] {
                null, new TreeSet<Integer>(), new TreeSet<String>()
        };

        Class<?>[] elementTypes = new Class[] {
                Integer.class, null, Void.TYPE
        };

        Class<? extends Exception>[] expectedExceptions = new Class[] {
                NullPointerException.class, NullPointerException.class, IllegalArgumentException.class
        };

        return Stream.of(Arguments.of(
                elements, elementTypes, expectedExceptions
        ));
    }
}
