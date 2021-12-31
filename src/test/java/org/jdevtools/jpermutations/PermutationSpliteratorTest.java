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
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.jdevtools.factorial.Factorials.longFactorial;
import static org.jdevtools.jpermutations.PermutationIteratorTest.getUnorderedPermutation;

/**
 * Class for testing spliterator of {@link Permutation}
 */
public class PermutationSpliteratorTest {

    @ParameterizedTest
    @MethodSource({
            "org.jdevtools.jpermutations.PermutationIteratorTest#_emptySet_Data",
            "org.jdevtools.jpermutations.PermutationIteratorTest#_singletonSet_Data",
            "org.jdevtools.jpermutations.PermutationIteratorTest#_twoData_Set",
            "org.jdevtools.jpermutations.PermutationIteratorTest#_threeData_Set",
            "org.jdevtools.jpermutations.PermutationIteratorTest#_fourData_Set"
    })
    public <T> void tryAdvance_Success(Set<T> elements, Class<T> elementType, Set<List<T>> expectedPermutations) {
        IPermutation<T> permutation = getUnorderedPermutation(elements, elementType);
        Spliterator<List<T>> spliterator = permutation.spliterator();

        Consumer<List<T>> containsAndRemove = list -> {
            Assertions.assertTrue(expectedPermutations.contains(list));
            expectedPermutations.remove(list);
        };

        while(spliterator.tryAdvance(containsAndRemove)) {}

        Assertions.assertTrue(expectedPermutations.isEmpty());
    }

    @ParameterizedTest
    @MethodSource({
            "org.jdevtools.jpermutations.PermutationIteratorTest#_emptySet_Data",
            "org.jdevtools.jpermutations.PermutationIteratorTest#_singletonSet_Data",
            "org.jdevtools.jpermutations.PermutationIteratorTest#_twoData_Set",
            "org.jdevtools.jpermutations.PermutationIteratorTest#_threeData_Set",
            "org.jdevtools.jpermutations.PermutationIteratorTest#_fourData_Set"
    })
    public <T> void trySplit_Success(Set<T> elements, Class<T> elementType, Set<List<T>> expectedPermutations) {
        IPermutation<T> permutation = getUnorderedPermutation(elements, elementType);
        Set<List<T>> actualPermutations = StreamSupport.stream(permutation.spliterator(), true)
                .peek(list -> Assertions.assertTrue(expectedPermutations.contains(list)))
//                .peek(list -> System.out.println(Thread.currentThread().getName()))
                .collect(Collectors.toSet());

        Assertions.assertEquals(expectedPermutations, actualPermutations);
    }

    @ParameterizedTest
    @MethodSource({
            "_trySplit_Success_DataSet_1",
            "_trySplit_Success_DataSet_2",
            "_trySplit_Success_DataSet_3"
    })
    public <T> void trySplit_Success(Set<T> elements, Class<T> elementType, long expectedCount, long skip) {
        IPermutation<T> permutation = getUnorderedPermutation(elements, elementType);
        long actualCount = StreamSupport.stream(permutation.spliterator(), true)
                .skip(skip)
                .count();

        Assertions.assertEquals(expectedCount, actualCount);
    }

    private static Stream<Arguments> getLongDataSet(int maxSize, int skip) {
        Set<Integer> elements = IntStream.range(0, maxSize).boxed().collect(Collectors.toSet());
        Class<Integer> elementType = Integer.class;
        long count = longFactorial(maxSize).get() - skip;

        return Stream.of(Arguments.of(
                elements, elementType, count, skip
        ));
    }

    public static Stream<Arguments> _trySplit_Success_DataSet_1() {
        return getLongDataSet(12, 0);
    }

    public static Stream<Arguments> _trySplit_Success_DataSet_2() {
        return getLongDataSet(15, 10);
    }

    public static Stream<Arguments> _trySplit_Success_DataSet_3() {
        return getLongDataSet(20, 100);
    }

    private static <T> Permutation<T> getOrderedPermutation(Set<T> elements, Class<T> elementType, Comparator<T> comparator) {
        return Permutation.ordered(elements, elementType, comparator);
    }

    @ParameterizedTest
    @MethodSource({
            "_orderedIteration_emptyData",
            "_orderedIteration_singletonData",
            "_orderedIteration_twoData",
            "_orderedIteration_threeData",
            "_orderedIteration_fourData",
    })
    public <T> void orderedSequentialIteration_Success_1(Set<T> elements, Class<T> elementType,
                                                         Comparator<T> comparator, List<T>[] expectedPermutations) {
        IPermutation<T> permutation = getOrderedPermutation(elements, elementType, comparator);
        Spliterator<List<T>> spliterator = permutation.spliterator();
        int[] index = {0};

        Consumer<List<T>> assertConsumer = actualPermutation -> {
            Assertions.assertEquals(expectedPermutations[index[0]], actualPermutation);
            index[0]++;
        };

        while(spliterator.tryAdvance(assertConsumer)) {}
    }

    public static Stream<Arguments> _orderedIteration_emptyData() {
        Set<?> elements = Set.of();
        Class<?> elementType = Integer.class;
        Comparator<?> comparator = null;

        List<?>[] expectedPermutations = new List<?>[] {List.of()};

        return Stream.of(Arguments.of(
                elements, elementType, comparator, expectedPermutations
        ));
    }

    public static Stream<Arguments> _orderedIteration_singletonData() {
        Set<String> elements = Set.of("set");
        Class<String> elementType = String.class;
        Comparator<?> comparator = null;

        List<String>[] expectedPermutations = new List[]{
                List.of("set")
        };

        return Stream.of(Arguments.of(
                elements, elementType, comparator, expectedPermutations
        ));
    }

    public static Stream<Arguments> _orderedIteration_twoData() {
        Set<Character> elements = Set.of('a', 'c');
        Class<Character> elementType = Character.class;
        Comparator<Character> comparator = Comparator.naturalOrder();

        List<Character>[] expectedPermutations = new List[] {
                List.of('a', 'c'),
                List.of('c', 'a')
        };

        return Stream.of(Arguments.of(
                elements, elementType, comparator, expectedPermutations
        ));
    }

    public static Stream<Arguments> _orderedIteration_threeData() {
        Set<Double> elements = Set.of(3d, 5.3, -23.5);
        Class<Double> elementType = Double.class;
        Comparator<Double> comparator = Comparator.reverseOrder();

        List<Double>[] expectedPermutations = new List[] {
                List.of(5.3, 3d, -23.5),
                List.of(5.3, -23.5, 3d),
                List.of(3d, 5.3, -23.5),
                List.of(3d, -23.5, 5.3),
                List.of(-23.5, 5.3, 3d),
                List.of(-23.5, 3d, 5.3)
        };

        return Stream.of(Arguments.of(
                elements, elementType, comparator, expectedPermutations
        ));
    }

    public static Stream<Arguments> _orderedIteration_fourData() {
        Set<Integer> elements = Set.of(4, -3, 6, -5);
        Class<Integer> elementType = Integer.class;
        Comparator<Integer> absComparator = Comparator.comparingLong(Math::abs);

        List<Integer>[] expectedPermutations = new List[] {
                List.of(-3, 4, -5, 6), List.of(-3, 4, 6, -5), List.of(-3, -5, 4, 6), List.of(-3, -5, 6, 4),
                List.of(-3, 6, 4,-5), List.of(-3, 6, -5, 4), List.of(4, -3, -5, 6), List.of(4, -3, 6, -5),
                List.of(4, -5, -3, 6), List.of(4, -5, 6, -3), List.of(4, 6, -3, -5), List.of(4, 6, -5, -3),
                List.of(-5, -3, 4, 6), List.of(-5, -3, 6, 4), List.of(-5, 4, -3, 6), List.of(-5, 4, 6, -3),
                List.of(-5, 6, -3, 4), List.of(-5, 6, 4, -3), List.of(6, -3, 4, -5), List.of(6, -3, -5, 4),
                List.of(6, 4, -3, -5), List.of(6, 4, -5, -3), List.of(6, -5, -3, 4), List.of(6, -5, 4, -3)
        };

        return Stream.of(Arguments.of(
                elements, elementType, absComparator, expectedPermutations
        ));
    }

    @ParameterizedTest
    @MethodSource({
            "_orderedIteration_emptyData",
            "_orderedIteration_singletonData",
            "_orderedIteration_twoData",
            "_orderedIteration_threeData",
            "_orderedIteration_fourData"
    })
    public <T> void orderedParallelIteration_Success(Set<T> elements, Class<T> elementType,
                                                     Comparator<T> comparator, List<List<T>>[] expectedPermutations) {
        IPermutation<T> permutation = getOrderedPermutation(elements, elementType, comparator);
        List<T>[] actualPermutations = wrapUncheckedLists
                (StreamSupport.stream(permutation.spliterator(), true).toArray(List[]::new)
        );

        Assertions.assertArrayEquals(expectedPermutations, actualPermutations);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T>[] wrapUncheckedLists(List<?>[] lists) {
        return (List<T>[]) lists;
    }
}
