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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Class for testing iterator of class {@link Permutation}.
 */
public class PermutationIteratorTest {

    @ParameterizedTest
    @MethodSource({
            "_emptySet_Data",
            "_singletonSet_Data",
            "_twoData_Set",
            "_threeData_Set",
            "_fourData_Set"
    })
    public <T> void permutationTest_Success(Set<T> elements, Class<T> elementType, Set<List<T>> resultPermutations) {
        IPermutation<T> permutation = getUnorderedPermutation(elements, elementType);

        for (List<T> permutations: permutation) {
            Assertions.assertTrue(resultPermutations.contains(permutations));
            resultPermutations.remove(permutations);
        }

        Assertions.assertTrue(resultPermutations.isEmpty());
    }

    public static <T> IPermutation<T> getUnorderedPermutation(Set<T> elements, Class<T> elementType) {
        return Permutation.unordered(elements, elementType);
    }

    public static Stream<Arguments> _emptySet_Data() {
        Set<?> emptySet = Collections.emptySet();
        Class<?> elementType = Object.class;
        Set<List<?>> resultPermutations = new HashSet<>(Set.of(Collections.emptyList()));

        return Stream.of(Arguments.of(
                emptySet,
                elementType,
                resultPermutations
        ));
    }

    public static Stream<Arguments> _singletonSet_Data() {
        Set<Integer> singletonSet = Collections.singleton(1);
        Class<Integer> elementType = Integer.class;
        Set<List<Integer>> resultPermutations = new HashSet<>(Set.of(List.of(1)));

        return Stream.of(Arguments.of(
                singletonSet,
                elementType,
                resultPermutations
        ));
    }

    public static Stream<Arguments> _twoData_Set() {
        Set<Integer> elements = Set.of(1, 2);
        Class<Integer> elementType = Integer.class;

        Set<List<Integer>> resultPermutations =
                new HashSet<>(Set.of(List.of(1, 2), List.of(2, 1)));

        return Stream.of(Arguments.of(
                elements,
                elementType,
                resultPermutations
        ));
    }

    public static Stream<Arguments> _threeData_Set() {
        Set<Integer> elements = Set.of(1, 2, 3);
        Class<Integer> elementType = Integer.class;

        Set<List<Integer>> resultPermutations =
                new HashSet<>(Set.of(List.of(1, 2, 3), List.of(1, 3, 2), List.of(2, 1, 3),
                        List.of(2, 3, 1), List.of(3, 1, 2), List.of(3, 2, 1)));

        return Stream.of(Arguments.of(
                elements,
                elementType,
                resultPermutations
        ));
    }

    public static Stream<Arguments> _fourData_Set() {
        Set<Integer> elements = Set.of(1, 2, 3, 4);
        Class<Integer> elementType = Integer.class;

        Set<List<Integer>> resultPermutations =
            new HashSet<>(
                Set.of(List.of(1, 2, 3, 4), List.of(1, 2, 4, 3), List.of(1, 3, 2, 4),
                       List.of(1, 3, 4, 2), List.of(1, 4, 2, 3), List.of(1, 4, 3, 2),
                       List.of(2, 1, 3, 4), List.of(2, 1, 4, 3), List.of(2, 3, 1, 4),
                       List.of(2, 3, 4, 1), List.of(2, 4, 1, 3), List.of(2, 4, 3, 1),
                       List.of(3, 1, 2, 4), List.of(3, 1, 4, 2), List.of(3, 2, 1, 4),
                       List.of(3, 2, 4, 1), List.of(3, 4, 1, 2), List.of(3, 4, 2, 1),
                       List.of(4, 1, 2, 3), List.of(4, 1, 3, 2), List.of(4, 2, 1, 3),
                       List.of(4, 2, 3, 1), List.of(4, 3, 1, 2), List.of(4, 3, 2, 1))
            );

        return Stream.of(Arguments.of(
                elements,
                elementType,
                resultPermutations
        ));
    }
}
