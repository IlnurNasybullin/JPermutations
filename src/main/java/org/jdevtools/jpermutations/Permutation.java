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

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.jdevtools.factorial.FactorialNumberSystems.decimal2IntFactorials;
import static org.jdevtools.factorial.Factorials.factorial;
import static org.jdevtools.factorial.Factorials.longFactorial;

/**
 * Class for iterating through all possible permutations (<b>without repetitions</b>) of a given set of elements. For
 * iteration through all permutations can be carried with <b>thread-unsafe</b> iterator {@link PermutationIterator} or
 * with <b>partially thread-safe</b> spliterator {@link PermutationSpliterator}. However, <b>this class is thread-safe</b>,
 * because it's immutable.
 * @implNote Classes {@link PermutationIterator} and {@link PermutationSpliterator} doesn't override Object's methods
 * {@link Object#equals(Object)}, {@link Object#hashCode()}, {@link Object#toString()} since these classes aren't intended
 * for equality comparison, storing in anything collection or converting to {@link String} for printing.
 * @param <T> type of the element
 * @author Ilnur Nasybullin
 */
public class Permutation<T> implements Iterable<List<T>>, IPermutation<T> {

    /**
     * Elements of permutations.
     */
    private final T[] elements;

    /**
     * {@link Comparator} for comparing elements (using for iterations permutations in lexicographic order by {@link PermutationSpliterator})
     * @implNote if this comparator is null, iterations won't be in lexicographic order
     */
    private final Comparator<? super T> comparator;

    /**
     * Class iterator for iterating all permutations.
     * <p><b>This class is thread-unsafe.</b></p>
     * @implNote For iterating all permutations is used
     * (<a href=https://en.wikipedia.org/wiki/Steinhaus–Johnson–Trotter_algorithm#Even's_speedup>Steinhaus-Johnson-Trotter algorithm with Even's speedup</a>).
     */
    private class PermutationIterator implements Iterator<List<T>> {

        private final int[] indexes;
        private final byte[] directions;
        private int currentIndex;
        private boolean isFirst;

        private final static byte LEFT_DIRECTION = Byte.MIN_VALUE;
        private final static byte RIGHT_DIRECTION = Byte.MAX_VALUE;
        private final static byte NO_DIRECTION = 0;

        private PermutationIterator() {
            this.indexes = new int[elements.length];
            this.directions = new byte[elements.length];
            this.currentIndex = 0;
            this.isFirst = true;
        }

        @Override
        public boolean hasNext() {
            return currentIndex != -1;
        }

        @Override
        public List<T> next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No such permutations!");
            }

            if (isFirst) {
                initIndexes();
                initDirections();
                isFirst = false;
            } else {
                nextIndexes();
            }

            return getElements();
        }

        private void initIndexes() {
            for (int i = 0; i < indexes.length; i++) {
                indexes[i] = i;
            }
            currentIndex = indexes.length > 1 ? indexes.length - 1 : -1;
        }

        private void initDirections() {
            for (int i = 1; i < directions.length; i++) {
                directions[i] = LEFT_DIRECTION;
            }
        }

        private void nextIndexes() {
            byte direction = directions[currentIndex];
            moveIndex(direction);

            boolean isBorder = isBorder(direction);
            if (isBorder || currentIndex != maxIndex()) {
                if (isBorder) {
                    directions[currentIndex] = NO_DIRECTION;
                }

                currentIndex = getMaxDirectedIndex();
                if (currentIndex == -1) {
                    return;
                }

                if (indexes[currentIndex] != maxIndex()) {
                    changeDirections(currentIndex);
                }
            }
        }

        private void moveIndex(byte direction) {
            if (direction == LEFT_DIRECTION) {
                swap(currentIndex, currentIndex - 1);
                currentIndex--;
            } else {
                swap(currentIndex, currentIndex + 1);
                currentIndex++;
            }
        }

        private void swap(int i, int j) {
            Permutation.swap(indexes, i, j);

            directions[i] ^= directions[j];
            directions[j] ^= directions[i];
            directions[i] ^= directions[j];
        }

        private boolean isBorder(byte direction) {
            return direction == NO_DIRECTION ||
                    (direction == LEFT_DIRECTION && !hasLeftMove() ||
                     direction == RIGHT_DIRECTION && !hasRightMove());
        }

        private boolean hasLeftMove() {
            return currentIndex > 0 && indexes[currentIndex] > indexes[currentIndex - 1];
        }

        private boolean hasRightMove() {
            return currentIndex < maxIndex() && indexes[currentIndex] > indexes[currentIndex + 1];
        }

        private int maxIndex() {
            return indexes.length - 1;
        }

        private int getMaxDirectedIndex() {
            int maxIndex = -1;
            int maxIndexValue = -1;

            int indexValue;
            for (int i = 0; i < indexes.length; i++) {
                indexValue = indexes[i];

                if (directions[i] != NO_DIRECTION && indexValue > maxIndexValue) {
                    maxIndexValue = indexValue;
                    maxIndex = i;
                }
            }

            return maxIndex;
        }

        private void changeDirections(int index) {
            int indexValue = indexes[index];

            for (int i = 0; i < index; i++) {
                if (indexes[i] > indexValue) {
                    directions[i] = RIGHT_DIRECTION;
                }
            }

            for (int i = index + 1; i < indexes.length; i++) {
                if (indexes[i] > indexValue) {
                    directions[i] = LEFT_DIRECTION;
                }
            }
        }

        private List<T> getElements() {
            List<T> permutation = new ArrayList<>(elements.length);
            for (int index: indexes) {
                permutation.add(elements[index]);
            }

            return permutation;
        }
    }

    private static void swap(int[] array, int i, int j) {
        array[i] ^= array[j];
        array[j] ^= array[i];
        array[i] ^= array[j];
    }

    /**
     * Spliterator for iteration all permutations (<b>without repetitions</b>). This spliterator is a <b>partially
     * thread-safe</b> - it can be used safely in parallel streams (this safety is guaranteed with specification of the
     * use of the spliterators in streams {@link Spliterator}, with using {@link #trySplit()} and giving new {@link Spliterator}
     * object to new thread), but it's not thread-safe, if two or more threads will be use same object of this class.
     * <b>Don't have use the same object of this class in different threads!</b> In the best case, an exception
     * {@link ConcurrentModificationException} will be thrown (see {@link #tryAdvance(Consumer)}), in the worst case
     * program will be worked unpredictable (spliterators won't iterate all permutations, there will be matching permutations during iterate).
     * @implNote This class for iteration all permutation is used
     * <a href = https://en.wikipedia.org/wiki/Permutation#Algorithms_to_generate_permutations>Narayana Pandita algorithm</a>,
     * generation in lexicographic order. However, {@link #elements} can be incomparable ({@link #comparator} is null),
     * so for maintaining lexicographic order is used indexes of {@link #elements}.
     */
    private class PermutationSpliterator implements Spliterator<List<T>> {

        /**
         * Indexes of the {@link #elements} by which the permutations are iterated in lexicographic order. An algorithm
         * point of view, this array is current permutation.
         */
        private final int[] indexes;

        /**
         * Current position of permutation. An algorithm point of view, this value is a factorial ID of permutations.
         * @see <a href=https://en.wikipedia.org/wiki/Factorial_number_system#Permutations>Factorial number system</a>
         */
        private BigInteger currentPosition;

        /**
         * End (exclusive) position of permutation.
         */
        private final BigInteger endPosition;

        /**
         * It's guaranteed bitmask of characteristics:
         * <ol>
         *     <li>{@link Spliterator#DISTINCT} - all permutations are distinct (there are no repetitions)</li>
         *     <li>{@link Spliterator#NONNULL} - for all set of {@link #elements} generated at least one permutation (for
         *     empty array of {@link #elements} generates empty list)</li>
         *     <li>{@link Spliterator#SIZED} - this spliterator can calculate own size (see {@link #estimateSize()})</li>
         *     <li>{@link Spliterator#SUBSIZED} - spliterator's children can also calculate own size (see {@link #estimateSize()})</li>
         *     <li>{@link Spliterator#IMMUTABLE} - source of elements ({@link #elements}) is not changed during iteration</li>
         *     <li>{@link Spliterator#CONCURRENT} - spliterator can be used in parallel computation (for example, in
         *     {@link Stream#parallel()}), but only based according to specification with using {@link Spliterator}</li>
         *     <li>{@link Spliterator#ORDERED} - spliterator generate permutations' lists according to lexicographic order
         *     of indexes of elements (see {@link Permutation.PermutationSpliterator})</li>
         * </ol>
         * @see Permutation.PermutationSpliterator
         */
        private static final int REQUIRED_CHARACTERISTICS = Spliterator.DISTINCT | Spliterator.NONNULL |
                Spliterator.SIZED | Spliterator.IMMUTABLE | Spliterator.CONCURRENT | Spliterator.SUBSIZED |
                Spliterator.ORDERED;

        /**
         * It's an additional bitmask of characteristics. This value can take two values:
         * <ul>
         *     <li>0 - no additional property of spliterator</li>
         *     <li>{@link Spliterator#SORTED} - spliterator iterates all permutations in lexicographic order</li>
         * </ul>
         * @see #listComparator
         */
        private final int additionalCharacteristics;

        /**
         * This comparator defines lexicographic order for permutations on based element's {@link #comparator}.
         * @implNote This value is nullable - if there is no need for lexicographic order.
         * @see #comparator
         */
        private final Comparator<List<T>> listComparator;

        private PermutationSpliterator(int[] indexes, BigInteger currentPosition, BigInteger endPosition,
                                       Comparator<List<T>> listComparator) {
            this.indexes = indexes;
            this.currentPosition = currentPosition;
            this.endPosition = endPosition;
            this.additionalCharacteristics = listComparator == null ? 0 : Spliterator.SORTED;
            this.listComparator = listComparator;
        }

        /**
         * Union of iterator methods: {@link Iterator#hasNext()} and  {@link Iterator#next()} with adding function for
         * processing the next elements.
         * <p>
         *     <b>This method is thread-unsafe</b> - two or more threads doesn't have to call this method on the same object!
         *     (see {@link PermutationSpliterator})
         * </p>
         * @param action function for processing the following elements
         * @return true - if action is performed, or false in otherwise
         * @throws NullPointerException if action is null
         * @throws ConcurrentModificationException can be thrown, if the same object's states can be modified by two or
         * more threads concurrently.
         * @implNote From a technical point of view, an exception {@link IllegalStateException} is thrown
         * that is caught and wrapped by exception {@link ConcurrentModificationException}. Exception {@link IllegalStateException}
         * can be thrown out in two situations:
         * <ol>
         *     <li>index (i) of element in array {@link #indexes}, whose neighbor on the right is greater that it, was not
         *     found (see {@link #findMaxIndex()})</li>
         *     <li>index of element in array {@link #indexes}, that greater than i and whose element is greater than indexes[i],
         *     was not found (see {@link #findMaxElementIndex(int)})</li>
         * </ol>
         * However, an algorithm point of view, there should be no exceptions during generation of permutations. So, these
         * exceptions occur as a result incorrect (illegal) states of variables ({@link #indexes} or {@link #currentPosition}) of
         * this object. The variables can be appeared in incorrect states only when modifying own states by another thread,
         * so an exception {@link ConcurrentModificationException} is thrown.
         * @see Spliterator#tryAdvance(Consumer)
         */
        @Override
        public boolean tryAdvance(Consumer<? super List<T>> action) {
            if (!hasNext()) {
                return false;
            }

            List<T> list = getList();
            action.accept(list);

            currentPosition = currentPosition.add(BigInteger.ONE);

            try {
                nextIndexes();
            } catch (IllegalStateException e) {
                throw new ConcurrentModificationException("Spliterator's states was modified by another thread!", e);
            }

            return true;
        }

        private boolean hasNext() {
            return currentPosition.compareTo(endPosition) < 0;
        }

        private void nextIndexes() {
            if (!hasNext()) {
                return;
            }

            int i = findMaxIndex();
            int j = findMaxElementIndex(i);

            swap(indexes, i, j);
            reverseFrom(i+1, indexes.length);
        }

        private void reverseFrom(int from, int to) {
            to--;
            while (from < to) {
                swap(indexes, from, to);
                from++;
                to--;
            }
        }

        private int findMaxElementIndex(int j) {
            int value = indexes[j];
            for (int i = indexes.length - 1; i > j; i--) {
                if (indexes[i] > value) {
                    return i;
                }
            }


            throw new IllegalStateException(
                    String.format("Not found max index of an element, that is greater than value %d", value));
        }

        private int findMaxIndex() {
            for (int i = indexes.length - 2; i >= 0; i--) {
                if (indexes[i] < indexes[i + 1]) {
                    return i;
                }
            }

            throw new IllegalStateException
                    ("The maximum index of an element whose neighbor on the right is greater than was not found!");
        }

        private List<T> getList() {
            List<T> list = new ArrayList<>(indexes.length);
            for (int index: indexes) {
                list.add(elements[index]);
            }

            return list;
        }

        /**
         * Returns new spliterator {@link PermutationSpliterator}, if at the moment of the method call, spliterator's size
         * {@link #estimateSize()} is more than 1; in otherwise returns null (in according to specification of using of spliterator)
         * <p>
         *     <b>This method is thread-unsafe</b> - two or more threads doesn't have to call this method on the same object!
         *     (see {@link PermutationSpliterator})
         * </p>
         * @return new spliterator, if {@link #estimateSize()} > 1 and null in otherwise
         * @implNote
         * <ol>
         *      <li>In according to specification of {@link Spliterator#trySplit()} after calling this method, it's guaranteed
         *      that if returned new spliterator (not null):
         *      <ul>
         *          <li>new spliterator will generate the first "half" (the left part) of the permutations (if size of
         *          current iterator doesn't fit in the long type ({@link #estimateSize()} > {@link Long#MAX_VALUE}), the
         *          new iterator will have the size <= {@link Long#MAX_VALUE} / 2)</li>
         *          <li>current spliterator will generate the second "half" (the part left, it's lexicographic greater than
         *          left part) of the permutations</li>
         *          <li>new spliterator will have limited {@link #estimateSize()} (<= {@link Long#MAX_VALUE} / 2)</li>
         *          <li>in limited size of current spliterator, sizes of new and current spliterators will be balanced
         *          (the difference between sizes won't greater than 1)</li>
         *      </ul>
         *      </li>
         *      <li>From a technical point of view, an exception {@link IllegalStateException} can be thrown, however, in
         *      practice, this doesn't happen even in concurrently modification of this object's states (but an exception
         *      {@link ArrayIndexOutOfBoundsException} can be thrown; see {@link #createNewBase(BigInteger)})</li>
         * </ol>
         * @see Spliterator#trySplit()
         */
        @Override
        public Spliterator<List<T>> trySplit() {
            long size = estimateSize();
            if (size < 2) {
                return null;
            }

            size /= 2;
            BigInteger end = currentPosition.add(BigInteger.valueOf(size));
            int[] indexes = Arrays.copyOf(this.indexes, this.indexes.length);
            Spliterator<List<T>> spliterator = new PermutationSpliterator(indexes, currentPosition, end, listComparator);
            currentPosition = end;

            createNewBase(end);

            return spliterator;
        }

        /**
         * Creating new indexes base (filling array {@link #indexes} with new values, that defined new permutation) on
         * based new current position
         * @param position new position of permutation
         * @implNote
         * <ol>
         *     <li>New permutation defined by transforming giving position from decimal number system to
         *     <a href=https://en.wikipedia.org/wiki/Factorial_number_system>factorial number system</a></li>
         *     <li>Despite the fact that from a theoretical point of view this method can be throw an exception
         *     {@link IllegalStateException} ({@link #getNotNegativeIndexOfElement(int[], int)}), on practise, for this
         *     case, one of two events must occur:
         *     <ul>
         *         <li>Changed the size of the array {@link #indexes}</li>
         *         <li>{@link #currentPosition} must be negative value</li>
         *     </ul>
         *     These cases are impossible without the using of Reflection API
         *     </li>
         *     <li>However, in concurrent calling of method {@link #trySplit()}, an exception {@link IndexOutOfBoundsException}
         *     can be thrown</li>
         * </ol>
         * @see #indexes
         * @see <a href=https://en.wikipedia.org/wiki/Factorial_number_system>Factorial number system</a>
         * @see org.jdevtools.factorial.FactorialNumberSystems#decimal2IntFactorials(BigInteger)
         */
        private void createNewBase(BigInteger position) {
            int[] mods = new int[indexes.length];
            int[] remainders = decimal2IntFactorials(position);

            // exception IndexOutOfBoundsException can be thrown
            System.arraycopy(remainders, 0, mods, 1, remainders.length);

            int[] sortedIndexes = getNaturalNumberSeries(0, indexes.length);
            int index;
            for (int j = mods.length - 1; j >= 0; j--) {
                index = getNotNegativeIndexOfElement(sortedIndexes, mods[j]);
                indexes[mods.length - 1 - j] = index;
                sortedIndexes[index] = -1;
            }
        }

        private int getNotNegativeIndexOfElement(int[] array, int searchIndex) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] < 0) {
                    continue;
                }

                if (searchIndex == 0) {
                    return i;
                }

                searchIndex--;
            }

            throw new IllegalStateException("Index with not negative element is not founded!");
        }

        /**
         * Returns estimate size of spliterator. In according to spliterator's specification - if spliterator's size less
         * than {@link Long#MAX_VALUE}, it will be returned current spliterator's size, in otherwise will be returned
         * {@link Long#MAX_VALUE}.
         * @return estimate size of spliterator, or, if spliterator's size more than {@link Long#MAX_VALUE}, will be returned
         * {@link Long#MAX_VALUE}
         * @implNote real size of spliterator is calculated as difference between {@link #endPosition} and {@link #currentPosition}
         * @see Spliterator#estimateSize()
         * @see #currentPosition
         */
        @Override
        public long estimateSize() {
            BigInteger size = size();
            if (size.bitLength() > Long.SIZE - 1) {
                return Long.MAX_VALUE;
            }

            return size.longValue();
        }

        private BigInteger size() {
            return endPosition.subtract(currentPosition);
        }

        /**
         * Bitmask that characterizes some properties of spliterator
         * @return bitmask of characteristics
         * @see #REQUIRED_CHARACTERISTICS
         * @see #additionalCharacteristics
         */
        @Override
        public int characteristics() {
            return REQUIRED_CHARACTERISTICS | additionalCharacteristics;
        }

        /**
         * Returns comparator of lists for lexicographic order of permutations
         * @return comparator for lexicographic order
         * @throws IllegalStateException if comparator is null
         * @see Spliterator#getComparator()
         */
        @Override
        public Comparator<? super List<T>> getComparator() {
            if (listComparator != null) {
                return listComparator;
            }

            throw new IllegalStateException("Spliterator doesn't support sorting of elements!");
        }
    }

    private Permutation(T[] elements, Comparator<? super T> comparator) {
        this.elements = elements;
        this.comparator = comparator;
    }

    /**
     * Static method for constructing {@link Permutation} object. There is no guarantee of permutations' iteration for
     * {@link #spliterator()}.
     * @param elements set of elements
     * @param elementType class-type of elements
     * @param <T> type of element
     * @return {@link Permutation} object
     * @throws NullPointerException if elements or elementType is null
     * @throws IllegalArgumentException if elementType is {@link Void#TYPE}
     * @throws ArrayStoreException if elementType is not a supertype of the runtime type of every element in set
     * @implNote
     * <ol>
     *     <li>elements of set are copied in {@link #elements} with method {@link Set#toArray(Object[])}</li>
     *     <li>Throwing an exception {@link IllegalArgumentException} follows from the implementation of the method
     *     {@link Array#newInstance(Class, int)}</li>
     * </ol>
     */
    public static <T> Permutation<T> unordered(Set<T> elements, Class<T> elementType) {
        T[] array = getArray(elementType, elements);
        return new Permutation<>(array, null);
    }

    /**
     * Static method for constructing {@link Permutation} object. There is a guarantee of permutations' iteration for
     * {@link #spliterator()}.
     * @param elements set of elements
     * @param elementType class-type of elements
     * @param comparator for comparing elements; it's used for lexicographic order of permutations; it's nullable, if
     *                   elements are comparable (implements interface {@link Comparable})
     * @param <T> type of element
     * @return {@link Permutation} object
     * @throws NullPointerException if elements or elementType is null
     * @throws ArrayStoreException if elementType is not a supertype of the runtime type of every element in set
     * @throws IllegalArgumentException is thrown in three cases:
     * <ul>
     *     <li>comparator is null and elements is incomparable (type isn't implement interface {@link Comparable})</li>
     *     <li>(optional) comparator (or {@link Comparable} interface realization) violates {@link Comparator} (or {@link Comparable})
     *     contract</li>
     *     <li>if elementType is {@link Void#TYPE}</li>
     * </ul>
     * @apiNote if elements is comparable (type implements interface {@link Comparable}) and comparator isn't null then
     * preferences will be given to comparator.
     * @implNote
     * <ol>
     *      <li>elements of set are copied in new array with method {@link Set#toArray(Object[])}</li>
     *      <li>created array sorted with method {@link Arrays#sort(Object[], Comparator)}</li>
     *      <li>The second case of throwing an exception {@link IllegalArgumentException} follows from the implementation
     *      {@link Arrays#sort(Object[], Comparator)}</li>
     *      <li>The third case of throwing an exception {@link IllegalArgumentException}  follows from the implementation
     *      of the method {@link Array#newInstance(Class, int)}</li>
     * </ol>
     */
    public static <T> Permutation<T> ordered(Set<T> elements, Class<T> elementType, Comparator<T> comparator) {
        Comparator<? super T> realComparator = getComparator(elementType, comparator);
        T[] array = getArray(elementType, elements);
        Arrays.sort(array, realComparator);

        return new Permutation<>(array, realComparator);
    }

    /**
     * Static method for constructing {@link Permutation} object. There is guarantee of permutations' iteration for
     * {@link #spliterator()}.
     * @param elements set of elements
     * @param elementType class-type for elements
     * @param <T> type of element
     * @return {@link Permutation} object
     * @throws NullPointerException if elements or elementType is null
     * @throws ArrayStoreException if elementType isn't a supertype of the runtime type of every element in set
     * @throws IllegalArgumentException if elementType is {@link Void#TYPE}
     * @implNote
     * <ol>
     *      <li>elements of set are copied in array with method {@link SortedSet#toArray(Object[])} that, according to the
     *      specification, guarantees that the array is filled in sorted form (using a comparator, or in natural order, if
     *      comparator is null)</li>
     *      <li>Throwing an exception {@link IllegalArgumentException} follows from the implementation of the method
     *      {@link Array#newInstance(Class, int)}</li>
     * </ol>
     */
    public static <T> Permutation<T> ordered(SortedSet<T> elements, Class<T> elementType) {
        Comparator<? super T> realComparator = getComparator(elementType, elements.comparator());
        T[] array = getArray(elementType, elements);
        return new Permutation<>(array, realComparator);
    }

    private static <T> Comparator<? super T> getComparator(Class<T> elementType, Comparator<? super T> comparator) {
        if (comparator != null) {
            return comparator;
        }

        if (Comparable.class.isAssignableFrom(elementType)) {
            return wrapUncheckedComparator(Comparator.naturalOrder());
        }

        throw new IllegalArgumentException(String.format("Comparator is null and %s is not implements Comparable interface!", elementType));
    }

    @SuppressWarnings("unchecked")
    private static <T> Comparator<T> wrapUncheckedComparator(Comparator<?> comparator) {
        return (Comparator<T>) comparator;
    }

    private static <T> T[] getArray(Class<T> elementType, Set<T> elements) {
        T[] array = wrapUncheckedArray(Array.newInstance(elementType, elements.size()));
        elements.toArray(array);
        return array;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] wrapUncheckedArray(Object array) {
        return (T[]) array;
    }

    /**
     * Returns <b>thread-unsafe</b> iterator through all possible permutations (<b>without repetitions</b>) of a set of elements.
     * For empty set also return iterator with one element - empty list, because for 0 elements exist only one
     * permutation (0! = 1).
     * @return iterator through all possible permutations
     * @apiNote It's recommended to use this iterator for <b>sequential iteration if there is no need to preserve lexicographic
     * order</b>
     * @see PermutationIterator
     */
    @Override
    public Iterator<List<T>> iterator() {
        return new PermutationIterator();
    }

    /**
     * Returns <b>partially thread-safe</b> spliterator through all possible permutations (<b>without repetitions</b>) of
     * a set of elements. For empty set also return spliterator with one element - empty list, because for 0 elements exist
     * only one permutation (0! = 1)
     * @return spliterator through all possible permutations
     * @apiNote It's recommended to use this iterator for <b>parallel iteration in {@link Stream#parallel()}</b> or
     * <b>if preservation of lexicographic order is necessary</b>
     * @see PermutationSpliterator
     */
    @Override
    public Spliterator<List<T>> spliterator() {
        int[] array = getNaturalNumberSeries(0, elements.length);
        Comparator<List<T>> listComparator = getListComparator(this.comparator);

        return new PermutationSpliterator(array, BigInteger.ZERO, size(), listComparator);
    }

    /**
     * Returns lexicographic order {@link Comparator} for {@link List}
     * @param comparator elements comparator
     * @param <T> type of element
     * @return lexicographic order {@link Comparator} for {@link List}
     */
    private static <T> Comparator<List<T>> getListComparator(final Comparator<? super T> comparator) {
        if (comparator == null) {
            return null;
        }

        return (list1, list2) -> {
            Iterator<T> iterator1 = list1.iterator();
            Iterator<T> iterator2 = list2.iterator();

            T el1;
            T el2;
            int compareTo;

            while (iterator1.hasNext() && iterator2.hasNext()) {
                el1 = iterator1.next();
                el2 = iterator2.next();

                compareTo = comparator.compare(el1, el2);
                if (compareTo != 0) {
                    return compareTo;
                }
            }

            boolean hasNext1 = iterator1.hasNext();
            boolean hasNext2 = iterator2.hasNext();

            if (hasNext1 ^ hasNext2) {
                return hasNext1 ? 1 : -1;
            }

            return 0;
        };
    }

    private static int[] getNaturalNumberSeries(int startInclusive, int endExclusive) {
        return IntStream.range(startInclusive, endExclusive).toArray();
    }

    /**
     * Returns number of all possible permutations such as long type, if it's possible, or will be returned
     * {@link Optional#empty()}.
     * @return number of all possible permutations, if it's possible, or {@link Optional#empty()}
     * @implNote The number of all possible permutations is actually equal to factorial of the number of elements, so
     * for calculation the number of all possible permutations is used static method {@link org.jdevtools.factorial.Factorials#factorial(int)}.
     * @see org.jdevtools.factorial.Factorials#longFactorial(int)
     */
    @Override
    public Optional<Long> longSize() {
        return longFactorial(elements.length);
    }

    /**
     * Returns number of all possible permutations.
     * @return number of all possibler permutations
     * @implNote The number of all possible permutations is actually equal to factorial of the number of elements, so for
     * calculation the number of all permutations is used static method {@link org.jdevtools.factorial.Factorials#factorial(int)}.
     * @see org.jdevtools.factorial.Factorials#factorial(int)
     */
    @Override
    public BigInteger size() {
        return factorial(elements.length);
    }

    /**
     * Returns true if two objects of {@link Permutation} is equal. For comparison, the sets of passed elements are
     * compared for equality.
     * @param o an object for comparison on the equivalence relation
     * @return true if two objects is equal
     * @implNote elements' sets are created with static method {@link Set#of(Object[])}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permutation<?> that = (Permutation<?>) o;

        return Set.of(elements).equals(Set.of(that.elements));
    }

    /**
     * Returns hash code of this object. Hash code is calculated for the sets of passed elements.
     * @return hash code of this object
     * @implNote
     * <ol>
     *     <li>set of elements is created with static method {@link Set#of(Object[])}</li>
     *     <li>for hash code's calculating is used static method {@link Objects#hash(Object...)}</li>
     * </ol>
     */
    @Override
    public int hashCode() {
        return Objects.hash(Set.of(elements));
    }
}
