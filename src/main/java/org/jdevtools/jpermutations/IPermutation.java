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

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * Interface for testing {@link Permutation} class. Is can be removed in future versions.
 * @param <T> - type of permutation's element
 */
public interface IPermutation<T> extends Iterable<List<T>> {

    /**
     * Returns count of all permutations in long type, if it's possible; otherwise it will be returned {@link Optional#empty()}
     * @return count of all permutations in long type
     */
    Optional<Long> longSize();

    /**
     * Returns count of all permutations
     * @return count of all permutations
     */
    BigInteger size();

}