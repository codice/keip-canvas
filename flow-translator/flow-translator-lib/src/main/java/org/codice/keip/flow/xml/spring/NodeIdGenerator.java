/*
 * Original code from JNanoID, licensed under the MIT License (see below)
 * Copyright (c) 2017 The JNanoID Authors
 * Copyright (c) 2017 Aventrix LLC
 * Copyright (c) 2017 Andrey Sitnik
 *
 * ----------------------------------------------------------------------
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 * ----------------------------------------------------------------------
 */
package org.codice.keip.flow.xml.spring;

import java.util.Random;

/**
 * A class for generating unique String IDs.
 *
 * <p>Implementation based on JNanoID (<a href="https://github.com/aventrix/jnanoid">...</a>)
 */
public class NodeIdGenerator {

  private NodeIdGenerator() {}

  public static final Random DEFAULT_NUMBER_GENERATOR = new Random();

  public static final char[] DEFAULT_ALPHABET =
      "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

  public static final int DEFAULT_SIZE = 10;

  /**
   * Static factory to retrieve a url-friendly, pseudo randomly generated, NanoId String.
   *
   * <p>The generated NanoId String will have 10 symbols.
   *
   * <p>The NanoId String is generated using {@link Random}, so ids are NOT cryptographically
   * secure.
   *
   * @return A randomly generated NanoId String.
   */
  public static String randomId() {
    return randomId(DEFAULT_NUMBER_GENERATOR, DEFAULT_ALPHABET, DEFAULT_SIZE);
  }

  /**
   * Static factory to retrieve a NanoId String.
   *
   * <p>The string is generated using the given random number generator.
   *
   * @param random The random number generator.
   * @param alphabet The symbols used in the NanoId String.
   * @param size The number of symbols in the NanoId String.
   * @return A randomly generated NanoId String.
   */
  public static String randomId(final Random random, final char[] alphabet, final int size) {
    if (random == null) {
      throw new IllegalArgumentException("random cannot be null.");
    }

    if (alphabet == null) {
      throw new IllegalArgumentException("alphabet cannot be null.");
    }

    if (alphabet.length == 0 || alphabet.length >= 256) {
      throw new IllegalArgumentException("alphabet must contain between 1 and 255 symbols.");
    }

    if (size <= 0) {
      throw new IllegalArgumentException("size must be greater than zero.");
    }

    final int mask = (2 << (int) Math.floor(Math.log(alphabet.length - 1) / Math.log(2))) - 1;
    final int step = (int) Math.ceil(1.6 * mask * size / alphabet.length);

    final StringBuilder idBuilder = new StringBuilder();

    while (true) {

      final byte[] bytes = new byte[step];
      random.nextBytes(bytes);

      for (int i = 0; i < step; i++) {

        final int alphabetIndex = bytes[i] & mask;

        if (alphabetIndex < alphabet.length) {
          idBuilder.append(alphabet[alphabetIndex]);
          if (idBuilder.length() == size) {
            return idBuilder.toString();
          }
        }
      }
    }
  }
}
